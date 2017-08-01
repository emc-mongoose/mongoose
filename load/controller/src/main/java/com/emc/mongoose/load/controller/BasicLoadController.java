package com.emc.mongoose.load.controller;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.concurrent.Coroutine;
import com.emc.mongoose.api.common.net.Service;
import com.emc.mongoose.api.common.net.ServiceUtil;
import com.emc.mongoose.api.metrics.logging.IoTraceCsvLogMessage;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.api.common.concurrent.RateThrottle;
import com.emc.mongoose.api.common.concurrent.ThreadUtil;
import com.emc.mongoose.api.common.concurrent.WeightThrottle;
import com.emc.mongoose.api.model.svc.RoundRobinOutputCoroutine;
import com.emc.mongoose.api.model.DaemonBase;
import com.emc.mongoose.api.model.io.task.IoTask.Status;
import com.emc.mongoose.api.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.api.model.io.task.path.PathIoTask;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import com.emc.mongoose.api.model.NamingThreadFactory;
import com.emc.mongoose.api.common.concurrent.Throttle;
import com.emc.mongoose.api.metrics.logging.IoTraceCsvBatchLogMessage;
import com.emc.mongoose.api.metrics.MetricsManager;
import com.emc.mongoose.api.common.io.Output;
import com.emc.mongoose.api.metrics.BasicMetricsContext;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.svc.TransferCoroutine;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import com.emc.mongoose.ui.config.test.step.limit.fail.FailConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.metrics.MetricsContext;
import com.emc.mongoose.ui.log.Loggers;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 12.07.16.
 */
public class BasicLoadController<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadController<I, O> {
	
	private final String name;
	private final Int2ObjectMap<LoadGenerator<I, O>> generatorsMap;
	private final Map<LoadGenerator<I, O>, List<StorageDriver<I, O>>> driversMap;
	private final long countLimit;
	private final long sizeLimit;
	private final long failCountLimit;
	private final boolean failRateLimitFlag;
	private final ConcurrentMap<I, O> latestIoResultsPerItem;
	private final int batchSize;
	private final boolean isAnyCircular;

	private final Int2ObjectMap<MetricsContext> ioStats = new Int2ObjectOpenHashMap<>();
	private final LongAdder counterResults = new LongAdder();
	private final Int2IntMap concurrencyMap;
	private final Int2IntMap driversCountMap;
	private final Throttle<Object> rateThrottle;
	private final WeightThrottle weightThrottle;
	private final Int2ObjectMap<Output<O>> ioTaskOutputs = new Int2ObjectOpenHashMap<>();
	private final boolean tracePersistFlag;
	
	private volatile Output<O> ioResultsOutput;
	
	/**
	 @param name test step name
	 @param driversMap generator to drivers list map
	 **/
	public BasicLoadController(
		final String name, final Map<LoadGenerator<I, O>, List<StorageDriver<I, O>>> driversMap,
		final Int2IntMap weightMap, final Map<LoadGenerator<I, O>, LoadConfig> loadConfigs,
		final StepConfig stepConfig, final Map<LoadGenerator<I, O>, OutputConfig> outputConfigs
	) {
		this.name = name;
		final LoadConfig firstLoadConfig = loadConfigs.values().iterator().next();
		final double rateLimit = firstLoadConfig.getRateConfig().getLimit();
		if(rateLimit > 0) {
			rateThrottle = new RateThrottle<>(rateLimit);
		} else {
			rateThrottle = null;
		}
		
		if(weightMap == null || weightMap.size() == 0 || weightMap.size() == 1) {
			weightThrottle = null;
		} else {
			weightThrottle = new WeightThrottle(weightMap);
		}

		generatorsMap = new Int2ObjectOpenHashMap<>(driversMap.size());
		Output<O> nextGeneratorOutput = null;
		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			// hashCode() returns the origin code
			generatorsMap.put(nextGenerator.hashCode(), nextGenerator);

			try {
				nextGeneratorOutput = new RoundRobinOutputCoroutine<>(
					driversMap.get(nextGenerator), nextGenerator.getSvcCoroutines(),
					nextGenerator.getBatchSize()
				);
			} catch(final RemoteException ignored) {
			}
			ioTaskOutputs.put(nextGenerator.hashCode(), nextGeneratorOutput);
			nextGenerator.setWeightThrottle(weightThrottle);
			nextGenerator.setRateThrottle(rateThrottle);
			nextGenerator.setOutput(nextGeneratorOutput);
		}

		final MetricsConfig anyMetricsConfig = outputConfigs
			.values().iterator().next().getMetricsConfig();
		tracePersistFlag = anyMetricsConfig.getTraceConfig().getPersist();

		this.driversMap = driversMap;
		concurrencyMap = new Int2IntOpenHashMap(driversMap.size());
		driversCountMap = new Int2IntOpenHashMap(driversMap.size());
		this.batchSize = firstLoadConfig.getBatchConfig().getSize();
		boolean anyCircularFlag = false;
		for(final LoadGenerator<I, O> nextGenerator : generatorsMap.values()) {
			final List<StorageDriver<I, O>> nextDrivers = driversMap.get(nextGenerator);
			final MetricsConfig nextMetricsConfig = outputConfigs
				.get(nextGenerator).getMetricsConfig();
			final LoadConfig nextLoadConfig = loadConfigs.get(nextGenerator);
			final int nextOriginCode = nextGenerator.hashCode();
			if(nextGenerator.isRecycling()) {
				anyCircularFlag = true;
			}
			final String ioTypeName = nextLoadConfig.getType().toUpperCase();
			final IoType ioType = IoType.valueOf(ioTypeName);
			final int ioTypeCode = ioType.ordinal();
			driversCountMap.put(ioTypeCode, nextDrivers.size());
			int ioTypeSpecificConcurrency = 0;
			try {
				ioTypeSpecificConcurrency = nextDrivers.get(0).getConcurrencyLevel();
				concurrencyMap.put(ioTypeCode, ioTypeSpecificConcurrency);
			} catch(final RemoteException e) {
				LogUtil.exception(Level.ERROR, e, "Failed to invoke the remote method");
			}
			ioStats.put(
				ioTypeCode,
				new BasicMetricsContext(
					name, ioType, nextDrivers.size(), ioTypeSpecificConcurrency,
					(int) (ioTypeSpecificConcurrency * nextMetricsConfig.getThreshold()),
					nextGenerator.getTransferSizeEstimate(),
					(int) nextMetricsConfig.getAverageConfig().getPeriod(),
					outputConfigs.get(nextGenerator).getColor(),
					nextMetricsConfig.getAverageConfig().getPersist(),
					nextMetricsConfig.getSummaryConfig().getPersist(),
					nextMetricsConfig.getSummaryConfig().getPerfDbResultsFileFlag()
				)
			);
		}
		this.isAnyCircular = anyCircularFlag;
		if(isAnyCircular) {
			final int
				recycleLimit = firstLoadConfig.getGeneratorConfig().getRecycleConfig().getLimit();
			latestIoResultsPerItem = new ConcurrentHashMap<>(recycleLimit);
		} else {
			latestIoResultsPerItem = null;
		}

		final LimitConfig limitConfig = stepConfig.getLimitConfig();
		this.countLimit = limitConfig.getCount() > 0 ? limitConfig.getCount() : Long.MAX_VALUE;
		this.sizeLimit = limitConfig.getSize().get() > 0 ?
			limitConfig.getSize().get() : Long.MAX_VALUE;
		final FailConfig failConfig = limitConfig.getFailConfig();
		this.failCountLimit = failConfig.getCount() > 0 ? failConfig.getCount() : Long.MAX_VALUE;
		this.failRateLimitFlag = failConfig.getRate();
	}

	private boolean isDoneCountLimit() {
		if(countLimit > 0) {
			if(counterResults.sum() >= countLimit) {
				Loggers.MSG.debug(
					"{}: count limit reached, {} results >= {} limit", name, counterResults.sum(),
					countLimit
				);
				return true;
			}
			long succCountSum = 0;
			long failCountSum = 0;
			MetricsContext.Snapshot lastStats;
			for(final int ioTypeCode : ioStats.keySet()) {
				lastStats = ioStats.get(ioTypeCode).getLastSnapshot();
				succCountSum += lastStats.getSuccCount();
				failCountSum += lastStats.getFailCount();
				if(succCountSum + failCountSum >= countLimit) {
					Loggers.MSG.debug(
						"{}: count limit reached, {} successful + {} failed >= {} limit",
						name, succCountSum, failCountSum, countLimit
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
			for(final int ioTypeCode : ioStats.keySet()) {
				sizeSum += ioStats.get(ioTypeCode).getLastSnapshot().getByteCount();
				if(sizeSum >= sizeLimit) {
					Loggers.MSG.debug(
						"{}: size limit reached, done {} >= {} limit",
						name, SizeInBytes.formatFixedSize(sizeSum), sizeLimit
					);
					return true;
				}
			}
		}
		return false;
	}

	private boolean allIoTasksCompleted() {
		long generatedIoTasks = 0;
		for(final LoadGenerator<I, O> generator : generatorsMap.values()) {
			try {
				if(generator.isInterrupted()) {
					generatedIoTasks += generator.getGeneratedTasksCount();
				} else {
					return false;
				}
			} catch(final RemoteException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to communicate with load generator \"{}\"",
					generator
				);
			}
		}
		return counterResults.longValue() >= generatedIoTasks;
	}

	// issue SLTM-938 fix
	private boolean nothingToRecycle() {
		if(generatorsMap.size() == 1) {
			final LoadGenerator<I, O> soleLoadGenerator = generatorsMap.values().iterator().next();
			try {
				if(soleLoadGenerator.isStarted()) {
					return false;
				}
			} catch(final RemoteException e) {
				LogUtil.exception(Level.WARN, e, "Failed to check the load generator state");
			}
			// load generator has done its work
			final long generatedIoTasks = soleLoadGenerator.getGeneratedTasksCount();
			if(
				soleLoadGenerator.isRecycling() &&
				counterResults.sum() >= generatedIoTasks && // all generated I/O tasks executed at least once
				latestIoResultsPerItem.size() == 0 // no successful I/O results
			) {
				return true;
			}
		}
		return false;
	}

	private boolean isDone() {
		if(isDoneCountLimit()) {
			Loggers.MSG.debug("{}: done due to max count done state", getName());
			return true;
		}
		if(isDoneSizeLimit()) {
			Loggers.MSG.debug("{}: done due to max size done state", getName());
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
		MetricsContext.Snapshot nextMetricsSnapshot;
		for(final int ioTypeCode : ioStats.keySet()) {
			nextMetricsSnapshot = ioStats.get(ioTypeCode).getLastSnapshot();
			failCountSum += nextMetricsSnapshot.getFailCount();
			failRateLast += nextMetricsSnapshot.getFailRateLast();
			succRateLast += nextMetricsSnapshot.getSuccRateLast();
		}
		if(failCountSum > failCountLimit) {
			Loggers.ERR.warn(
				"{}: failure count ({}) is more than the configured limit ({}), stopping the step",
				name, failCountSum, failCountLimit
			);
			return true;
		}
		if(failRateLimitFlag && failRateLast > succRateLast) {
			Loggers.ERR.warn(
				"{}: failures rate ({} failures/sec) is more than success rate ({} op/sec), " +
					"stopping the step",
				name, failRateLast, succRateLast
			);
			return true;
		}
		return false;
	}

	private boolean isIdle()
	throws ConcurrentModificationException {

		for(final LoadGenerator<I, O> nextLoadGenerator : driversMap.keySet()) {

			try {
				if(!nextLoadGenerator.isInterrupted() && !nextLoadGenerator.isClosed()) {
					return false;
				}
			} catch(final RemoteException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to communicate with load generator \"{}\"",
					nextLoadGenerator
				);
			}

			for(final StorageDriver<I, O> nextStorageDriver : driversMap.get(nextLoadGenerator)) {
				try {
					if(
						!nextStorageDriver.isClosed() && !nextStorageDriver.isInterrupted() &&
						!nextStorageDriver.isIdle()
					) {
						return false;
					}
				} catch(final NoSuchObjectException e) {
					if(!isClosed() && !isInterrupted()) {
						LogUtil.exception(
							Level.WARN, e, "Failed to communicate with storage driver \"{}\"",
							nextStorageDriver
						);
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to communicate with storage driver \"{}\"",
						nextStorageDriver
					);
				}
			}
		}

		return true;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final void setIoResultsOutput(final Output<O> ioTaskResultsOutput) {
		this.ioResultsOutput = ioTaskResultsOutput;
	}
	
	@Override
	public final boolean put(final O ioTaskResult) {
		
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
		
		final int ioTypeCode = ioTaskResult.getIoType().ordinal();
		final MetricsContext ioTypeStats = ioStats.get(ioTypeCode);
		final IoTask.Status status = ioTaskResult.getStatus();
		
		if(Status.SUCC.equals(status)) {
			final long reqDuration = ioTaskResult.getDuration();
			final long respLatency = ioTaskResult.getLatency();
			final long countBytesDone;
			if(ioTaskResult instanceof DataIoTask) {
				countBytesDone = ((DataIoTask) ioTaskResult).getCountBytesDone();
			} else if(ioTaskResult instanceof PathIoTask) {
				countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
			} else {
				countBytesDone = 0;
			}
			
			if(ioTaskResult instanceof PartialIoTask) {
				ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
			} else {
				final int originCode = ioTaskResult.getOriginCode();
				final LoadGenerator<I, O> loadGenerator = generatorsMap.get(originCode);
				if(loadGenerator.isRecycling()) {
					latestIoResultsPerItem.put(ioTaskResult.getItem(), ioTaskResult);
					loadGenerator.recycle(ioTaskResult);
				} else if(ioResultsOutput != null) {
					try {
						if(!ioResultsOutput.put(ioTaskResult)) {
							return false;
						}
					} catch(final EOFException e) {
						LogUtil.exception(
							Level.DEBUG, e, "I/O task destination end of input"
						);
					} catch(final NoSuchObjectException e) {
						LogUtil.exception(
							Level.DEBUG, e,
							"Remote I/O task destination is not more available"
						);
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "Failed to put the I/O task to the destionation"
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
		
		// I/O trace logging
		if(tracePersistFlag) {
			Loggers.IO_TRACE.info(new IoTraceCsvBatchLogMessage<>(ioTaskResults, from, to));
		}

		int originCode;
		LoadGenerator<I, O> loadGenerator;
		O ioTaskResult;
		int ioTypeCode;
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

			originCode = ioTaskResult.getOriginCode();
			ioTypeCode = ioTaskResult.getIoType().ordinal();
			status = ioTaskResult.getStatus();
			reqDuration = ioTaskResult.getDuration();
			respLatency = ioTaskResult.getLatency();
			if(ioTaskResult instanceof DataIoTask) {
				countBytesDone = ((DataIoTask) ioTaskResult).getCountBytesDone();
			} else if(ioTaskResult instanceof PathIoTask) {
				countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
			}

			ioTypeStats = ioStats.get(ioTypeCode);

			if(Status.SUCC.equals(status)) {
				if(ioTaskResult instanceof PartialIoTask) {
					ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
				} else {
					loadGenerator = generatorsMap.get(originCode);
					if(loadGenerator.isRecycling()) {
						latestIoResultsPerItem.put(ioTaskResult.getItem(), ioTaskResult);
						loadGenerator.recycle(ioTaskResult);
					} else if(ioResultsOutput != null){
						try {
							if(!ioResultsOutput.put(ioTaskResult)) {
								break;
							}
						} catch(final EOFException e) {
							LogUtil.exception(
								Level.DEBUG, e, "I/O task destination end of input"
							);
						} catch(final NoSuchObjectException e) {
							LogUtil.exception(
								Level.DEBUG, e,
								"Remote I/O task destination is not more available"
							);
						} catch(final IOException e) {
							LogUtil.exception(
								Level.WARN, e, "Failed to put the I/O task to the destionation"
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
	public final int getActiveTaskCount() {
		int totalActiveTaskCount = 0;
		synchronized(driversMap) {
			for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
				final List<StorageDriver<I, O>> nextGeneratorDrivers = driversMap.get(nextGenerator);
				for(final StorageDriver<I, O> nextDriver : nextGeneratorDrivers) {
					try {
						totalActiveTaskCount += nextDriver.getActiveTaskCount();
					} catch(final RemoteException e) {
						LogUtil.exception(Level.WARN, e, "Failed to invoke the remote method");
					}
				}
			}
		}
		return totalActiveTaskCount;
	}
	
	@Override
	protected void doStart()
	throws IllegalStateException {
		super.doStart();

		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O>> nextGeneratorDrivers = driversMap.get(nextGenerator);
			for(final StorageDriver<I, O> nextDriver : nextGeneratorDrivers) {
				try {
					nextDriver.start();
				} catch(final IllegalStateException | RemoteException e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to start the driver {}", nextDriver.toString()
					);
				}
			}
			try {
				nextGenerator.start();
			} catch(final IllegalStateException | RemoteException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to start the generator {}", nextGenerator.toString()
				);
			}
		}

		for(final int ioTypeCode : concurrencyMap.keySet()) {
			ioStats.get(ioTypeCode).start();
			try {
				MetricsManager.register(this, ioStats.get(ioTypeCode));
			} catch(final InterruptedException e) {
				throw new CancellationException(e.getMessage());
			} catch(final TimeoutException e) {
				LogUtil.exception(Level.WARN, e, "{}: metrics context registering timeout", name);
			}
		}

		for(final List<StorageDriver<I, O>> nextGeneratorDrivers : driversMap.values()) {
			for(final StorageDriver<I, O> nextDriver : nextGeneratorDrivers) {
				svcCoroutines.add(
					new TransferCoroutine<>(svcCoroutines, name, nextDriver, this, batchSize)
				);
			}
		}
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {
		
		final ExecutorService shutdownExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(),
			new NamingThreadFactory("shutdownWorker", true)
		);

		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			shutdownExecutor.submit(
				() -> {
					try(
						final Instance ctx = CloseableThreadContext
							.put(KEY_TEST_STEP_ID, name)
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
					) {
						nextGenerator.interrupt();
						Loggers.MSG.debug(
							"{}: load generator \"{}\" interrupted", getName(),
							nextGenerator.toString()
						);
					} catch(final RemoteException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to interrupt the generator {}", getName(),
							nextGenerator.toString()
						);
					}
				}
			);
			for(final StorageDriver<I, O> driver : driversMap.get(nextGenerator)) {
				shutdownExecutor.submit(
					() -> {
						try(
							final Instance ctx = CloseableThreadContext
								.put(KEY_TEST_STEP_ID, name)
								.put(KEY_CLASS_NAME, getClass().getSimpleName())
						) {
							driver.shutdown();
							Loggers.MSG.debug(
								"{}: next storage driver {} shutdown", getName(),
								(
									(driver instanceof Service)?
										((Service) driver).getName() + " @ " +
											ServiceUtil.getAddress((Service) driver) :
										driver.toString()
								)
							);
						} catch(final RemoteException e) {
							LogUtil.exception(
								Level.WARN, e, "failed to shutdown the driver {}", getName(),
								driver.toString()
							);
						}
					}
				);
			}
		}
		
		Loggers.MSG.info("{}: shutting down the storage drivers...", getName());
		shutdownExecutor.shutdown();
		try {
			if(shutdownExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				Loggers.MSG.debug("{}: load controller was shut down properly", getName());
			} else {
				Loggers.ERR.warn("{}: load controller shutdown timeout", getName());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "{}: load controller shutdown interrupted", getName());
		}
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		long t, timeOutMilliSec = timeUnit.toMillis(timeout);
		//
		Loggers.MSG.debug(
			"{}: await for the done condition at most for {}[s]", getName(),
			TimeUnit.MILLISECONDS.toSeconds(timeOutMilliSec)
		);
		t = System.currentTimeMillis();
		while(System.currentTimeMillis() - t < timeOutMilliSec) {
			synchronized(state) {
				state.wait(100);
			}
			if(isInterrupted()) {
				Loggers.MSG.debug("{}: await exit due to \"interrupted\" state", getName());
				return true;
			}
			if(isClosed()) {
				Loggers.MSG.debug("{}: await exit due to \"closed\" state", getName());
				return true;
			}
			if(isDone()) {
				Loggers.MSG.debug("{}: await exit due to \"done\" state", getName());
				return true;
			}
			if(isFailThresholdReached()) {
				Loggers.MSG.debug("{}: await exit due to \"BAD\" state", getName());
				return true;
			}
			synchronized(driversMap) {
				if(!isAnyCircular && allIoTasksCompleted()) {
					Loggers.MSG.debug(
						"{}: await exit because all I/O tasks have been completed", getName()
					);
					return true;
				}
				// issue SLTM-938 fix
				if(nothingToRecycle()) {
					Loggers.ERR.debug(
						"{}: exit because there's no I/O task to recycle (all failed)", getName()
					);
					return true;
				}
			}
		}
		Loggers.MSG.debug("{}: await exit due to timeout", getName());
		return false;
	}

	@Override
	protected final void doInterrupt()
	throws IllegalStateException {
		
		final ExecutorService interruptExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(),
			new NamingThreadFactory("interruptWorker", true)
		);

		synchronized(driversMap) {
			for(final LoadGenerator<I, O> nextGenerator : generatorsMap.values()) {
				for(final StorageDriver<I, O> driver : driversMap.get(nextGenerator)) {
					interruptExecutor.submit(
						() -> {
							try(
								final Instance ctx = CloseableThreadContext
									.put(KEY_TEST_STEP_ID, name)
									.put(KEY_CLASS_NAME, getClass().getSimpleName())
							) {
								driver.interrupt();
								Loggers.MSG.debug(
									"{}: next storage driver {} interrupted", getName(),
									(
										(driver instanceof Service)?
											((Service) driver).getName() + " @ " +
												ServiceUtil.getAddress((Service) driver) :
											driver.toString()
									)
								);
							} catch(final RemoteException e) {
								LogUtil.exception(
									Level.DEBUG, e, "{}: failed to interrupt the driver {}",
									getName(), driver.toString()
								);
							}
						}
					);
				}
			}
		}
		
		Loggers.MSG.info("{}: interrupting the storage drivers...", getName());
		interruptExecutor.shutdown();
		try {
			if(interruptExecutor.awaitTermination(100, TimeUnit.SECONDS)) {
				Loggers.MSG.debug("{}: storage drivers have been interrupted properly", getName());
			} else {
				Loggers.ERR.warn("{}: storage drivers interrupting timeout", getName());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(
				Level.WARN, e, "{}: storage drivers interrupting interrupted", getName()
			);
		}
		
		synchronized(svcCoroutines) {
			// stop all service coroutines
			for(final Coroutine svcCoroutine : svcCoroutines) {
				try {
					svcCoroutine.close();
				} catch(final IOException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to stop the service coroutine {}", svcCoroutine
					);
				}
			}
			svcCoroutines.clear();
		}

		final ExecutorService ioResultsExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(), new NamingThreadFactory("ioResultsWorker", true)
		);
		synchronized(driversMap) {
			for(final LoadGenerator<I, O> generator : generatorsMap.values()) {
				for(final StorageDriver<I, O> driver : driversMap.get(generator)) {
					ioResultsExecutor.submit(
						() -> {
							try(
								final Instance ctx = CloseableThreadContext
									.put(KEY_TEST_STEP_ID, name)
									.put(KEY_CLASS_NAME, getClass().getSimpleName())
							) {
								try {
									final List<O> finalResults = driver.getAll();
									if(finalResults != null) {
										final int finalResultsCount = finalResults.size();
										if(finalResultsCount > 0) {
											Loggers.MSG.debug(
												"{}: the driver \"{}\" returned {} final I/O " +
													"results to process",
												getName(), driver.toString(), finalResults.size()
											);
											put(finalResults, 0, finalResultsCount);
										}
									}
								} catch(final Throwable cause) {
									LogUtil.exception(
										Level.WARN, cause,
										"{}: failed to process the final results for the driver {}",
										getName(), driver.toString()
									);
								}
							}
						}
					);
				}
			}

			ioResultsExecutor.shutdown();

			try {
				if(ioResultsExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
					Loggers.MSG.debug(
						"{}: final I/O result have been got and processed properly", getName()
					);
				} else {
					Loggers.ERR.warn(
						"{}: timeout while getting and processing the final I/O results", getName()
					);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(
					Level.WARN, e,
					"{}: interrupted  while getting and processing the final I/O results", getName()
				);
			}
		}

		if(latestIoResultsPerItem != null && ioResultsOutput != null) {
			try {
				final int ioResultCount = latestIoResultsPerItem.size();
				Loggers.MSG.info(
					"{}: please wait while performing {} I/O results output...", name, ioResultCount
				);
				for(final O latestItemIoResult : latestIoResultsPerItem.values()) {
					try {
						if(!ioResultsOutput.put(latestItemIoResult)) {
							Loggers.ERR.debug(
								"{}: item info output fails to ingest, blocking the closing method",
								getName()
							);
							while(!ioResultsOutput.put(latestItemIoResult)) {
								Thread.sleep(1);
							}
							Loggers.MSG.debug("{}: closing method unblocked", getName());
						}
					} catch (final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to output the latest results", getName()
						);
					}
				}
			} catch(final InterruptedException ignored) {
			} finally {
				Loggers.MSG.info("{}: I/O results output done", name);
			}
			latestIoResultsPerItem.clear();
		}
		if(ioResultsOutput != null) {
			try {
				ioResultsOutput.put((O) null);
				Loggers.MSG.debug("{}: poisoned the items output", getName());
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "{}: failed to poison the results output", getName()
				);
			} catch(final NullPointerException e) {
				LogUtil.exception(
					Level.ERROR, e, "{}: results output \"{}\" failed to eat the poison", getName(),
					ioResultsOutput
				);
			}
		}

		for(final MetricsContext nextStats : ioStats.values()) {
			try {
				MetricsManager.unregister(this, nextStats);
			} catch(final InterruptedException | TimeoutException e) {
				LogUtil.exception(Level.WARN, e, "{}: metrics context unregister failure", name);
			}
		}

		Loggers.MSG.debug("{}: interrupted the load controller", getName());
	}

	@Override
	protected final void doClose()
	throws IOException {

		super.doClose();

		synchronized (driversMap) {

			for(final LoadGenerator<I, O> generator : driversMap.keySet()) {

				try {
					generator.close();
					Loggers.MSG.debug(
						"{}: the load generator \"{}\" has been closed", getName(), generator
					);
				} catch(final IOException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to close the generator {}", getName(), generator
					);
				}

				for(final StorageDriver<I, O> driver : driversMap.get(generator)) {
					try {
						driver.close();
						Loggers.MSG.debug("{}: next storage driver {} closed", getName(),
							((driver instanceof Service) ?
								((Service) driver).getName() + " @ " +
									ServiceUtil.getAddress((Service) driver) :
								driver.toString())
						);
					} catch(final NoSuchObjectException ignored) {
						// closing causes this normally
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to close the driver {}",
							getName(), driver.toString()
						);
					}
				}
			}

			generatorsMap.clear();
			driversMap.clear();
		}

		for(final Output<O> nextIoTaskOutput : ioTaskOutputs.values()) {
			nextIoTaskOutput.close();
		}
		ioTaskOutputs.clear();

		for(final MetricsContext nextStats : ioStats.values()) {
			nextStats.close();
		}
		ioStats.clear();

		Loggers.MSG.debug("{}: closed the load controller", getName());
	}
}
