package com.emc.mongoose.scenario.step.type;

import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.api.model.concurrent.DaemonBase;
import com.emc.mongoose.api.model.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.api.metrics.logging.IoTraceCsvLogMessage;
import com.emc.mongoose.api.model.io.task.IoTask.Status;
import com.emc.mongoose.api.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.api.model.io.task.path.PathIoTask;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import com.emc.mongoose.api.metrics.logging.IoTraceCsvBatchLogMessage;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import com.emc.mongoose.ui.config.test.step.limit.fail.FailConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.api.metrics.MetricsContext;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.commons.io.Output;

import com.github.akurilov.concurrent.coroutine.Coroutine;
import com.github.akurilov.concurrent.coroutine.TransferCoroutine;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 12.07.16.
 */
public class BasicLoadController<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadController<I, O> {
	
	private final String id;
	private final LoadGenerator<I, O> generator;
	private final StorageDriver<I, O> driver;
	private final long countLimit;
	private final long sizeLimit;
	private final long failCountLimit;
	private final boolean failRateLimitFlag;
	private final ConcurrentMap<I, O> latestIoResultByItem;
	private final boolean isRecycling;
	private final Coroutine resultsTransferCoroutine;
	private final MetricsContext metricsCtx;
	private final LongAdder counterResults = new LongAdder();
	private final boolean tracePersistFlag;
	private final int batchSize;

	private volatile Output<O> ioResultsOutput;
	
	/**
	 @param id test step id
	 **/
	public BasicLoadController(
		final String id,
		final LoadGenerator<I, O> generator,
		final StorageDriver<I, O> driver,
		final MetricsContext metricsCtx,
		final LimitConfig limitConfig,
		final boolean tracePersistFlag,
		final int batchSize,
		final int recycleLimit
	) {
		this.id = id;
		this.generator = generator;
		this.driver = driver;
		this.metricsCtx = metricsCtx;
		this.tracePersistFlag = tracePersistFlag;
		this.batchSize = batchSize;
		this.isRecycling = generator.isRecycling();
		if(isRecycling) {
			latestIoResultByItem = new ConcurrentHashMap<>(recycleLimit);
		} else {
			latestIoResultByItem = null;
		}
		resultsTransferCoroutine = new TransferCoroutine<>(
			ServiceTaskExecutor.INSTANCE, driver, this, batchSize
		);
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
					"{}: count limit reached, {} results >= {} limit", id, counterResults.sum(),
					countLimit
				);
				return true;
			}
			final MetricsSnapshot lastStats = metricsCtx.lastSnapshot();
			final long succCountSum = lastStats.succCount();
			final long failCountSum = lastStats.failCount();
			if(succCountSum + failCountSum >= countLimit) {
				Loggers.MSG.debug(
					"{}: count limit reached, {} successful + {} failed >= {} limit", id,
					succCountSum, failCountSum, countLimit
				);
				return true;
			}
		}
		return false;
	}

	private boolean isDoneSizeLimit() {
		if(sizeLimit > 0) {
			final long sizeSum = metricsCtx.lastSnapshot().byteCount();
			if(sizeSum >= sizeLimit) {
				Loggers.MSG.debug(
					"{}: size limit reached, done {} >= {} limit", id,
					SizeInBytes.formatFixedSize(sizeSum), sizeLimit
				);
				return true;
			}
		}
		return false;
	}

	private boolean allIoTasksCompleted() {
		try {
			if(generator.isStopped()) {
				return counterResults.longValue() >= generator.getGeneratedTasksCount();
			}
		} catch(final RemoteException ignored) {
		}
		return false;
	}

	// issue SLTM-938 fix
	private boolean nothingToRecycle() {
		try {
			if(generator.isStarted()) {
				return false;
			}
		} catch(final RemoteException ignored) {
		}
		// load generator has done its work
		final long generatedIoTasks = generator.getGeneratedTasksCount();
		return isRecycling &&
				// all generated I/O tasks executed at least once
				counterResults.sum() >= generatedIoTasks &&
				// no successful I/O results
				latestIoResultByItem.size() == 0;
	}

	private boolean isDone() {
		if(isDoneCountLimit()) {
			Loggers.MSG.debug("{}: done due to max count done state", id);
			return true;
		}
		if(isDoneSizeLimit()) {
			Loggers.MSG.debug("{}: done due to max size done state", id);
			return true;
		}
		return false;
	}

	/**
	 @return true if the configured failures threshold is reached and the step should be stopped,
	 false otherwise
	 */
	private boolean isFailThresholdReached() {
		final MetricsSnapshot metricsSnapshot = metricsCtx.lastSnapshot();
		final long failCountSum = metricsSnapshot.failCount();
		final double failRateLast = metricsSnapshot.failRateLast();
		final double succRateLast = metricsSnapshot.succRateLast();
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
		try {
			if(!generator.isStopped() && !generator.isClosed()) {
				return false;
			}
			if(!driver.isStopped() && !driver.isClosed() && !driver.isIdle()) {
				return false;
			}
		} catch(final RemoteException ignored) {
		}
		return true;
	}

	@Override
	public final void ioResultsOutput(final Output<O> ioTaskResultsOutput) {
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

		final Status status = ioTaskResult.status();
		if(Status.SUCC.equals(status)) {
			final long reqDuration = ioTaskResult.duration();
			final long respLatency = ioTaskResult.latency();
			final long countBytesDone;
			if(ioTaskResult instanceof DataIoTask) {
				countBytesDone = ((DataIoTask) ioTaskResult).countBytesDone();
			} else if(ioTaskResult instanceof PathIoTask) {
				countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
			} else {
				countBytesDone = 0;
			}
			
			if(ioTaskResult instanceof PartialIoTask) {
				metricsCtx.markPartSucc(countBytesDone, reqDuration, respLatency);
			} else {
				if(isRecycling) {
					latestIoResultByItem.put(ioTaskResult.item(), ioTaskResult);
					generator.recycle(ioTaskResult);
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
				metricsCtx.markSucc(countBytesDone, reqDuration, respLatency);
				counterResults.increment();
			}
		} else if(!Status.INTERRUPTED.equals(status)) {
			Loggers.ERR.debug("{}: {}", ioTaskResult.toString(), status.toString());
			metricsCtx.markFail();
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

		O ioTaskResult;
		Status status;
		long reqDuration;
		long respLatency;
		long countBytesDone = 0;

		int i;
		for(i = from; i < to; i++) {

			ioTaskResult = ioTaskResults.get(i);
			
			if( // account only completed composite I/O tasks
				ioTaskResult instanceof CompositeIoTask &&
					!((CompositeIoTask) ioTaskResult).allSubTasksDone()
			) {
				continue;
			}

			status = ioTaskResult.status();
			reqDuration = ioTaskResult.duration();
			respLatency = ioTaskResult.latency();
			if(ioTaskResult instanceof DataIoTask) {
				countBytesDone = ((DataIoTask) ioTaskResult).countBytesDone();
			} else if(ioTaskResult instanceof PathIoTask) {
				countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
			}

			if(Status.SUCC.equals(status)) {
				if(ioTaskResult instanceof PartialIoTask) {
					metricsCtx.markPartSucc(countBytesDone, reqDuration, respLatency);
				} else {
					if(isRecycling) {
						latestIoResultByItem.put(ioTaskResult.item(), ioTaskResult);
						generator.recycle(ioTaskResult);
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
					
					metricsCtx.markSucc(countBytesDone, reqDuration, respLatency);
					counterResults.increment();
				}
			} else if(!Status.INTERRUPTED.equals(status)) {
				Loggers.ERR.debug("{}: {}", ioTaskResult.toString(), status.toString());
				metricsCtx.markFail();
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
		try {
			resultsTransferCoroutine.start();
		} catch(final RemoteException ignored) {
		}
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		final long timeOutMilliSec = timeUnit.toMillis(timeout);
		Loggers.MSG.debug(
			"{}: await for the done condition at most for {}[s]", id,
			TimeUnit.MILLISECONDS.toSeconds(timeOutMilliSec)
		);
		final long t = System.currentTimeMillis();
		while(System.currentTimeMillis() - t < timeOutMilliSec) {
			if(super.await(100, TimeUnit.MILLISECONDS)) {
				return true;
			}
			if(isStopped()) {
				Loggers.MSG.debug("{}: await exit due to \"interrupted\" state", id);
				return true;
			}
			if(isClosed()) {
				Loggers.MSG.debug("{}: await exit due to \"closed\" state", id);
				return true;
			}
			if(isDone()) {
				Loggers.MSG.debug("{}: await exit due to \"done\" state", id);
				return true;
			}
			if(isFailThresholdReached()) {
				Loggers.MSG.debug("{}: await exit due to \"BAD\" state", id);
				return true;
			}
			if(!isRecycling && allIoTasksCompleted()) {
				Loggers.MSG.debug(
					"{}: await exit because all I/O tasks have been completed", id
				);
				return true;
			}
			// issue SLTM-938 fix
			if(nothingToRecycle()) {
				Loggers.ERR.debug(
					"{}: exit because there's no I/O task to recycle (all failed)", id
				);
				return true;
			}
		}
		Loggers.MSG.debug("{}: await exit due to timeout", id);
		return false;
	}

	@Override
	protected final void doStop()
	throws IllegalStateException {
		
		try {
			resultsTransferCoroutine.stop();
		} catch(final RemoteException ignored) {
		}

		try(
			final CloseableThreadContext.Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, id)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			try {
				final List<O> finalResults = driver.getAll();
				if(finalResults != null) {
					final int finalResultsCount = finalResults.size();
					if(finalResultsCount > 0) {
						Loggers.MSG.debug(
							"{}: the driver \"{}\" returned {} final I/O results to process", id,
							driver.toString(), finalResults.size()
						);
						for(int i = 0; i < finalResultsCount; i += batchSize) {
							put(finalResults, i, Math.min(i + batchSize, finalResultsCount));
						}
					}
				}
			} catch(final Throwable cause) {
				LogUtil.exception(
					Level.WARN, cause,
					"{}: failed to process the final results for the driver {}",
					id, driver.toString()
				);
			}
		}

		if(latestIoResultByItem != null && ioResultsOutput != null) {
			try {
				final int ioResultCount = latestIoResultByItem.size();
				Loggers.MSG.info(
					"{}: please wait while performing {} I/O results output...", id, ioResultCount
				);
				for(final O latestItemIoResult : latestIoResultByItem.values()) {
					try {
						if(!ioResultsOutput.put(latestItemIoResult)) {
							Loggers.ERR.debug(
								"{}: item info output fails to ingest, blocking the closing method",
								id
							);
							while(!ioResultsOutput.put(latestItemIoResult)) {
								Thread.sleep(1);
							}
							Loggers.MSG.debug("{}: closing method unblocked", id);
						}
					} catch (final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to output the latest results", id
						);
					}
				}
			} catch(final InterruptedException e) {
				throw new CancellationException(e.getMessage());
			} finally {
				Loggers.MSG.info("{}: I/O results output done", id);
			}
			latestIoResultByItem.clear();
		}
		if(ioResultsOutput != null) {
			try {
				ioResultsOutput.put((O) null);
				Loggers.MSG.debug("{}: poisoned the items output", id);
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "{}: failed to poison the results output", id
				);
			} catch(final NullPointerException e) {
				LogUtil.exception(
					Level.ERROR, e, "{}: results output \"{}\" failed to eat the poison", id,
					ioResultsOutput
				);
			}
		}

		Loggers.MSG.debug("{}: interrupted the load controller", id);
	}

	@Override
	protected final void doClose() {
		try {
			resultsTransferCoroutine.close();
		} catch(final IOException e) {
			LogUtil.exception(
				Level.WARN, e, "{}: failed to stop the service coroutine {}",
				resultsTransferCoroutine
			);
		}
		Loggers.MSG.debug("{}: closed the load controller", id);
	}
}
