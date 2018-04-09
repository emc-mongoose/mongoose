package com.emc.mongoose.load.controller;

import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.api.model.concurrent.DaemonBase;
import com.emc.mongoose.api.model.concurrent.ServiceTaskExecutor;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.concurrent.RateThrottle;
import com.github.akurilov.concurrent.Throttle;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.concurrent.ThreadUtil;
import com.github.akurilov.concurrent.WeightThrottle;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import com.github.akurilov.concurrent.coroutines.Coroutine;
import com.github.akurilov.concurrent.coroutines.TransferCoroutine;

import com.emc.mongoose.api.metrics.logging.IoTraceCsvLogMessage;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.api.model.io.task.IoTask.Status;
import com.emc.mongoose.api.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.api.model.io.task.path.PathIoTask;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.api.metrics.logging.IoTraceCsvBatchLogMessage;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.metrics.MetricsContext;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 12.07.16.
 */
public class BasicLoadController<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadController<I, O> {
	
	private final String id;
	private final Int2ObjectMap<LoadGenerator<I, O>> generatorByOrigin;
	private final Map<LoadGenerator<I, O>, StorageDriver<I, O>> driverByGenerator;
	private final long countLimit;
	private final long sizeLimit;
	private final long failCountLimit;
	private final boolean failRateLimitFlag;
	private final ConcurrentMap<I, O> latestIoResultsByItem;
	private final boolean isAnyCircular;
	private final List<Coroutine> resultsTransferCoroutines = new ArrayList<>();
	private final Int2ObjectMap<MetricsContext> metricsByOrigin;
	private final LongAdder counterResults = new LongAdder();
	private final boolean tracePersistFlag;

	private volatile Output<O> ioResultsOutput;
	
	/**
	 @param id test step id
	 @param driverByGenerator generator to drivers list map
	 **/
	public BasicLoadController(
		final String id, final Map<LoadGenerator<I, O>, StorageDriver<I, O>> driverByGenerator,
		final Int2IntMap weightMap, final Int2ObjectMap<MetricsContext> metricsByOrigin,
		final Map<LoadGenerator<I, O>, LoadConfig> loadConfigs, final StepConfig stepConfig,
		final Map<LoadGenerator<I, O>, OutputConfig> outputConfigs
	) {
		this.id = id;
		final var firstLoadConfig = loadConfigs.values().iterator().next();
		final var rateLimit = firstLoadConfig.getLimitConfig().getRate();
		final Throttle<Object> rateThrottle;
		if(rateLimit > 0) {
			rateThrottle = new RateThrottle<>(rateLimit);
		} else {
			rateThrottle = null;
		}
		final WeightThrottle weightThrottle;
		if(weightMap == null || weightMap.size() == 0 || weightMap.size() == 1) {
			weightThrottle = null;
		} else {
			weightThrottle = new WeightThrottle(weightMap);
		}
		this.metricsByOrigin = metricsByOrigin;

		generatorByOrigin = new Int2ObjectOpenHashMap<>(driverByGenerator.size());
		for(final var nextGenerator : driverByGenerator.keySet()) {
			// hashCode() returns the origin code
			generatorByOrigin.put(nextGenerator.hashCode(), nextGenerator);
			nextGenerator.setWeightThrottle(weightThrottle);
			nextGenerator.setRateThrottle(rateThrottle);
			nextGenerator.setOutput(driverByGenerator.get(nextGenerator));
		}

		final var anyMetricsConfig = outputConfigs.values().iterator().next().getMetricsConfig();
		tracePersistFlag = anyMetricsConfig.getTraceConfig().getPersist();

		this.driverByGenerator = driverByGenerator;
		final var batchSize = firstLoadConfig.getBatchConfig().getSize();
		var anyCircularFlag = false;
		for(final var nextGenerator : driverByGenerator.keySet()) {
			final var nextDriver = driverByGenerator.get(nextGenerator);
			if(nextGenerator.isRecycling()) {
				anyCircularFlag = true;
			}
			final var resultsTransferCoroutine = new TransferCoroutine<>(
				ServiceTaskExecutor.INSTANCE, nextDriver, this, batchSize
			);
			resultsTransferCoroutines.add(resultsTransferCoroutine);
		}

		this.isAnyCircular = anyCircularFlag;
		if(isAnyCircular) {
			final var
				recycleLimit = firstLoadConfig.getGeneratorConfig().getRecycleConfig().getLimit();
			latestIoResultsByItem = new ConcurrentHashMap<>(recycleLimit);
		} else {
			latestIoResultsByItem = null;
		}

		final var limitConfig = stepConfig.getLimitConfig();
		this.countLimit = limitConfig.getCount() > 0 ? limitConfig.getCount() : Long.MAX_VALUE;
		this.sizeLimit = limitConfig.getSize().get() > 0 ?
			limitConfig.getSize().get() : Long.MAX_VALUE;
		final var failConfig = limitConfig.getFailConfig();
		this.failCountLimit = failConfig.getCount() > 0 ? failConfig.getCount() : Long.MAX_VALUE;
		this.failRateLimitFlag = failConfig.getRate();
	}

	private boolean isDoneCountLimit() {
		if(countLimit > 0) {
			if(counterResults.sum() >= countLimit) {
				Loggers.MSG.debug(
					"{}: count limit reached, {} results >= {} limit", id, counterResults.sum(),
					countLimit
				);
				return true;
			}
			long succCountSum = 0;
			long failCountSum = 0;
			MetricsSnapshot lastStats;
			for(final var originCode : metricsByOrigin.keySet()) {
				lastStats = metricsByOrigin.get(originCode).lastSnapshot();
				succCountSum += lastStats.succCount();
				failCountSum += lastStats.failCount();
				if(succCountSum + failCountSum >= countLimit) {
					Loggers.MSG.debug(
						"{}: count limit reached, {} successful + {} failed >= {} limit", id,
						succCountSum, failCountSum, countLimit
					);
					return true;
				}
			}
		}
		return false;
	}

	private boolean isDoneSizeLimit() {
		if(sizeLimit > 0) {
			long sizeSum = 0;
			for(final var originCode : metricsByOrigin.keySet()) {
				sizeSum += metricsByOrigin.get(originCode).lastSnapshot().byteCount();
				if(sizeSum >= sizeLimit) {
					Loggers.MSG.debug(
						"{}: size limit reached, done {} >= {} limit", id,
						SizeInBytes.formatFixedSize(sizeSum), sizeLimit
					);
					return true;
				}
			}
		}
		return false;
	}

	private boolean allIoTasksCompleted() {
		long generatedIoTasks = 0;
		for(final var generator : generatorByOrigin.values()) {
			try {
				if(generator.isStopped()) {
					generatedIoTasks += generator.getGeneratedTasksCount();
				} else {
					return false;
				}
			} catch(final RemoteException ignored) {
			}
		}
		return counterResults.longValue() >= generatedIoTasks;
	}

	// issue SLTM-938 fix
	private boolean nothingToRecycle() {
		if(generatorByOrigin.size() == 1) {
			final var soleLoadGenerator = generatorByOrigin.values().iterator().next();
			try {
				if(soleLoadGenerator.isStarted()) {
					return false;
				}
			} catch(final RemoteException ignored) {
			}
			// load generator has done its work
			final var generatedIoTasks = soleLoadGenerator.getGeneratedTasksCount();
			if(
				soleLoadGenerator.isRecycling() &&
				counterResults.sum() >= generatedIoTasks && // all generated I/O tasks executed at least once
				latestIoResultsByItem.size() == 0 // no successful I/O results
			) {
				return true;
			}
		}
		return false;
	}

	private boolean isDone() {
		if(isDoneCountLimit()) {
			Loggers.MSG.debug("{}: done due to max count done state", id());
			return true;
		}
		if(isDoneSizeLimit()) {
			Loggers.MSG.debug("{}: done due to max size done state", id());
			return true;
		}
		return false;
	}

	/**
	 @return true if the configured failures threshold is reached and the step should be stopped,
	 false otherwise
	 */
	private boolean isFailThresholdReached() {
		long failCountSum = 0;
		double failRateLast = 0;
		double succRateLast = 0;
		MetricsSnapshot nextMetricsSnapshot;
		for(final var originCode : metricsByOrigin.keySet()) {
			nextMetricsSnapshot = metricsByOrigin.get(originCode).lastSnapshot();
			failCountSum += nextMetricsSnapshot.failCount();
			failRateLast += nextMetricsSnapshot.failRateLast();
			succRateLast += nextMetricsSnapshot.succRateLast();
		}
		if(failCountSum > failCountLimit) {
			Loggers.ERR.warn(
				"{}: failure count ({}) is more than the configured limit ({}), stopping the step",
				id, failCountSum, failCountLimit
			);
			return true;
		}
		if(failRateLimitFlag && failRateLast > succRateLast) {
			Loggers.ERR.warn(
				"{}: failures rate ({} failures/sec) is more than success rate ({} op/sec), " +
					"stopping the step", id, failRateLast, succRateLast
			);
			return true;
		}
		return false;
	}

	private boolean isIdle()
	throws ConcurrentModificationException {
		for(final var nextLoadGenerator : driverByGenerator.keySet()) {
			try {
				if(!nextLoadGenerator.isStopped() && !nextLoadGenerator.isClosed()) {
					return false;
				}
				final var nextStorageDriver = driverByGenerator.get(nextLoadGenerator);
				if(
					!nextStorageDriver.isStopped() && !nextStorageDriver.isClosed() &&
					!nextStorageDriver.isIdle()
				) {
					return false;
				}
			} catch(final RemoteException ignored) {
			}
		}
		return true;
	}

	@Override
	public final String id() {
		return id;
	}

	@Override
	public final void setIoResultsOutput(final Output<O> ioTaskResultsOutput) {
		this.ioResultsOutput = ioTaskResultsOutput;
	}
	
	@Override
	public final boolean put(final O ioTaskResult) {

		ThreadContext.put(KEY_TEST_STEP_ID, id);
		
		// I/O trace logging
		if(tracePersistFlag) {
			Loggers.IO_TRACE.info(new IoTraceCsvLogMessage<>(ioTaskResult));
		}
		
		if( // account only completed composite I/O tasks
			ioTaskResult instanceof CompositeIoTask &&
				!((CompositeIoTask) ioTaskResult).allSubTasksDone()
		) {
			return true;
		}

		final var originCode = ioTaskResult.originCode();
		final var ioTypeStats = metricsByOrigin.get(originCode);
		final var status = ioTaskResult.status();
		
		if(Status.SUCC.equals(status)) {
			final var reqDuration = ioTaskResult.duration();
			final var respLatency = ioTaskResult.latency();
			final long countBytesDone;
			if(ioTaskResult instanceof DataIoTask) {
				countBytesDone = ((DataIoTask) ioTaskResult).countBytesDone();
			} else if(ioTaskResult instanceof PathIoTask) {
				countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
			} else {
				countBytesDone = 0;
			}
			
			if(ioTaskResult instanceof PartialIoTask) {
				ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
			} else {
				final var loadGenerator = generatorByOrigin.get(originCode);
				if(loadGenerator.isRecycling()) {
					latestIoResultsByItem.put(ioTaskResult.item(), ioTaskResult);
					loadGenerator.recycle(ioTaskResult);
				} else if(ioResultsOutput != null) {
					try {
						if(!ioResultsOutput.put(ioTaskResult)) {
							Loggers.ERR.warn("Failed to output the I/O result");
						}
					} catch(final EOFException e) {
						LogUtil.exception(
							Level.DEBUG, e, "I/O task destination end of input"
						);
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "Failed to put the I/O task to the destination"
						);
					}
				}
				ioTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
				counterResults.increment();
			}
		} else if(!Status.INTERRUPTED.equals(status)) {
			Loggers.ERR.debug("{}: {}", ioTaskResult.toString(), status.toString());
			ioTypeStats.markFail();
			counterResults.increment();
		}

		return true;
	}
	
	@Override
	public final int put(final List<O> ioTaskResults, final int from, final int to) {

		ThreadContext.put(KEY_TEST_STEP_ID, id);
		
		// I/O trace logging
		if(tracePersistFlag) {
			Loggers.IO_TRACE.info(new IoTraceCsvBatchLogMessage<>(ioTaskResults, from, to));
		}

		int originCode;
		LoadGenerator<I, O> loadGenerator;
		O ioTaskResult;
		Status status;
		long reqDuration;
		long respLatency;
		long countBytesDone = 0;
		MetricsContext ioTypeStats;

		int i;
		for(i = from; i < to; i++) {

			ioTaskResult = ioTaskResults.get(i);
			
			if( // account only completed composite I/O tasks
				ioTaskResult instanceof CompositeIoTask &&
				!((CompositeIoTask) ioTaskResult).allSubTasksDone()
			) {
				continue;
			}

			originCode = ioTaskResult.originCode();
			status = ioTaskResult.status();
			reqDuration = ioTaskResult.duration();
			respLatency = ioTaskResult.latency();
			if(ioTaskResult instanceof DataIoTask) {
				countBytesDone = ((DataIoTask) ioTaskResult).countBytesDone();
			} else if(ioTaskResult instanceof PathIoTask) {
				countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
			}

			ioTypeStats = metricsByOrigin.get(originCode);

			if(Status.SUCC.equals(status)) {
				if(ioTaskResult instanceof PartialIoTask) {
					ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
				} else {
					loadGenerator = generatorByOrigin.get(originCode);
					if(loadGenerator.isRecycling()) {
						latestIoResultsByItem.put(ioTaskResult.item(), ioTaskResult);
						loadGenerator.recycle(ioTaskResult);
					} else if(ioResultsOutput != null) {
						try {
							if(!ioResultsOutput.put(ioTaskResult)) {
								Loggers.ERR.warn("Failed to output the I/O result");
							}
						} catch(final EOFException e) {
							LogUtil.exception(
								Level.DEBUG, e, "I/O task destination end of input"
							);
						} catch(final IOException e) {
							LogUtil.exception(
								Level.WARN, e, "Failed to put the I/O task to the destination"
							);
						}
					}
					
					ioTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
					counterResults.increment();
				}
			} else if(!Status.INTERRUPTED.equals(status)) {
				Loggers.ERR.debug("{}: {}", ioTaskResult.toString(), status.toString());
				ioTypeStats.markFail();
				counterResults.increment();
			}
		}
		
		return i - from;
	}
	
	@Override
	public final int put(final List<O> ioTaskResults) {
		return put(ioTaskResults, 0, ioTaskResults.size());
	}
	
	@Override
	protected void doStart()
	throws IllegalStateException {
		driverByGenerator
			.values()
			.forEach(
				driver -> {
					try {
						driver.start();
					} catch(final RemoteException ignored) {
					}
				}
			);
		driverByGenerator
			.keySet()
			.forEach(
				generator -> {
					try {
						generator.start();
					} catch(final RemoteException ignored) {
					}
				}
			);
		resultsTransferCoroutines
			.forEach(
				coroutine -> {
					try {
						coroutine.start();
					} catch(final RemoteException ignored) {
					}
				}
			);
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {
		
		final var shutdownExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(), new LogContextThreadFactory("shutdownWorker", true)
		);

		for(final var generator : driverByGenerator.keySet()) {
			shutdownExecutor.submit(
				() -> {
					try(
						final var ctx = CloseableThreadContext
							.put(KEY_TEST_STEP_ID, id)
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
					) {
						generator.stop();
						Loggers.MSG.debug(
							"{}: load generator \"{}\" interrupted", id(),
							generator.toString()
						);
					} catch(final RemoteException ignored) {
					}
				}
			);
			final var driver = driverByGenerator.get(generator);
			shutdownExecutor.submit(
				() -> {
					try(
						final var ctx = CloseableThreadContext
							.put(KEY_TEST_STEP_ID, id)
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
					) {
						driver.shutdown();
						Loggers.MSG.debug(
							"{}: next storage driver {} shutdown", id(), driver.toString()
						);
					} catch(final RemoteException ignored) {
					}
				}
			);
		}
		
		Loggers.MSG.debug("{}: shutting down the storage drivers...", id());
		shutdownExecutor.shutdown();
		try {
			if(shutdownExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				Loggers.MSG.debug("{}: load controller was shut down properly", id());
			} else {
				Loggers.ERR.warn("{}: load controller shutdown timeout", id());
			}
		} catch(final InterruptedException e) {
			throw new CancellationException(e.getMessage());
		}
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		final var timeOutMilliSec = timeUnit.toMillis(timeout);
		//
		Loggers.MSG.debug(
			"{}: await for the done condition at most for {}[s]", id(),
			TimeUnit.MILLISECONDS.toSeconds(timeOutMilliSec)
		);
		var t = System.currentTimeMillis();
		while(System.currentTimeMillis() - t < timeOutMilliSec) {
			synchronized(state) {
				state.wait(100);
			}
			if(isStopped()) {
				Loggers.MSG.debug("{}: await exit due to \"interrupted\" state", id());
				return true;
			}
			if(isClosed()) {
				Loggers.MSG.debug("{}: await exit due to \"closed\" state", id());
				return true;
			}
			if(isDone()) {
				Loggers.MSG.debug("{}: await exit due to \"done\" state", id());
				return true;
			}
			if(isFailThresholdReached()) {
				Loggers.MSG.debug("{}: await exit due to \"BAD\" state", id());
				return true;
			}
			synchronized(driverByGenerator) {
				if(!isAnyCircular && allIoTasksCompleted()) {
					Loggers.MSG.debug(
						"{}: await exit because all I/O tasks have been completed", id()
					);
					return true;
				}
				// issue SLTM-938 fix
				if(nothingToRecycle()) {
					Loggers.ERR.debug(
						"{}: exit because there's no I/O task to recycle (all failed)", id()
					);
					return true;
				}
			}
		}
		Loggers.MSG.debug("{}: await exit due to timeout", id());
		return false;
	}

	@Override
	protected final void doStop()
	throws IllegalStateException {
		
		final var interruptExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(),
			new LogContextThreadFactory("interruptWorker", true)
		);

		synchronized(driverByGenerator) {
			for(final var generator : driverByGenerator.keySet()) {
				final var driver = driverByGenerator.get(generator);
				interruptExecutor.submit(
					() -> {
						try(
							final var ctx = CloseableThreadContext
								.put(KEY_TEST_STEP_ID, id)
								.put(KEY_CLASS_NAME, getClass().getSimpleName())
						) {
							driver.stop();
							Loggers.MSG.debug(
								"{}: next storage driver {} interrupted", id(),
								driver.toString()
							);
						} catch(final RemoteException ignored) {
						}
					}
				);
			}
		}
		
		Loggers.MSG.debug("{}: interrupting the storage drivers...", id());
		interruptExecutor.shutdown();
		try {
			if(interruptExecutor.awaitTermination(100, TimeUnit.SECONDS)) {
				Loggers.MSG.debug("{}: storage drivers have been interrupted properly", id());
			} else {
				Loggers.ERR.warn("{}: storage drivers interrupting timeout", id());
			}
		} catch(final InterruptedException e) {
			throw new CancellationException(e.getMessage());
		}

		for(final var transferCoroutine : resultsTransferCoroutines) {
			try {
				transferCoroutine.stop();
			} catch(final RemoteException ignored) {
			}
		}

		final var ioResultsExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(),
			new LogContextThreadFactory("ioResultsWorker", true)
		);
		synchronized(driverByGenerator) {
			for(final var driver : driverByGenerator.values()) {
				ioResultsExecutor.submit(
					() -> {
						try(
							final var ctx = CloseableThreadContext
								.put(KEY_TEST_STEP_ID, id)
								.put(KEY_CLASS_NAME, getClass().getSimpleName())
						) {
							try {
								final var finalResults = driver.getAll();
								if(finalResults != null) {
									final var finalResultsCount = finalResults.size();
									if(finalResultsCount > 0) {
										Loggers.MSG.debug(
											"{}: the driver \"{}\" returned {} final I/O " +
												"results to process",
											id(), driver.toString(), finalResults.size()
										);
										put(finalResults, 0, finalResultsCount);
									}
								}
							} catch(final Throwable cause) {
								LogUtil.exception(
									Level.WARN, cause,
									"{}: failed to process the final results for the driver {}",
									id(), driver.toString()
								);
							}
						}
					}
				);
			}

			ioResultsExecutor.shutdown();

			try {
				if(ioResultsExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
					Loggers.MSG.debug(
						"{}: final I/O result have been got and processed properly", id()
					);
				} else {
					Loggers.ERR.warn(
						"{}: timeout while getting and processing the final I/O results", id()
					);
				}
			} catch(final InterruptedException e) {
				throw new CancellationException(e.getMessage());
			}
		}

		if(latestIoResultsByItem != null && ioResultsOutput != null) {
			try {
				final var ioResultCount = latestIoResultsByItem.size();
				Loggers.MSG.info(
					"{}: please wait while performing {} I/O results output...", id, ioResultCount
				);
				for(final O latestItemIoResult : latestIoResultsByItem.values()) {
					try {
						if(!ioResultsOutput.put(latestItemIoResult)) {
							Loggers.ERR.debug(
								"{}: item info output fails to ingest, blocking the closing method",
								id()
							);
							while(!ioResultsOutput.put(latestItemIoResult)) {
								Thread.sleep(1);
							}
							Loggers.MSG.debug("{}: closing method unblocked", id());
						}
					} catch (final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to output the latest results", id()
						);
					}
				}
			} catch(final InterruptedException e) {
				throw new CancellationException(e.getMessage());
			} finally {
				Loggers.MSG.info("{}: I/O results output done", id);
			}
			latestIoResultsByItem.clear();
		}
		if(ioResultsOutput != null) {
			try {
				ioResultsOutput.put((O) null);
				Loggers.MSG.debug("{}: poisoned the items output", id());
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "{}: failed to poison the results output", id()
				);
			} catch(final NullPointerException e) {
				LogUtil.exception(
					Level.ERROR, e, "{}: results output \"{}\" failed to eat the poison", id(),
					ioResultsOutput
				);
			}
		}

		Loggers.MSG.debug("{}: interrupted the load controller", id());
	}

	@Override
	protected final void doClose() {
		synchronized (driverByGenerator) {
			generatorByOrigin.clear();
			driverByGenerator.clear();
		}
		for(final var transferCoroutine : resultsTransferCoroutines) {
			try {
				transferCoroutine.close();
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "{}: failed to stop the service coroutine {}", transferCoroutine
				);
			}
		}
		resultsTransferCoroutines.clear();
		Loggers.MSG.debug("{}: closed the load controller", id());
	}
}
