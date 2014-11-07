package com.emc.mongoose.base.load.impl;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.base.load.StorageNodeExecutor;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import com.emc.mongoose.util.logging.MessageFactoryImpl;
import com.emc.mongoose.util.threading.GentleExecutorShutDown;
import com.emc.mongoose.util.threading.WorkerFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 15.10.14.
 */
public abstract class StorageNodeExecutorBase<T extends DataItem>
extends ThreadPoolExecutor
implements StorageNodeExecutor<T> {
	//
	private final Logger log;
	//
	protected final RequestConfig<T> localReqConf;
	private final Counter counterSubm, counterRej, counterReqSucc, counterReqFail;
	private final Counter counterSubmParent, counterRejParent, counterReqSuccParent, counterReqFailParent;
	private final Meter reqBytes, reqBytesParent;
	private final Histogram reqDur, reqDurParent;
	//
	private volatile Consumer<T> consumer = null;
	private final Lock lock = new ReentrantLock();
	private final Condition condInterrupted = lock.newCondition();
	//
	protected final RunTimeConfig runTimeConfig;
	private final Request.Type reqType;
	//
	protected StorageNodeExecutorBase(
		final RunTimeConfig runTimeConfig,
		final int threadsPerNode, final RequestConfig<T> localReqConf,
		final MetricRegistry parentMetrics, final String parentName, final Map<String,String> context
	) {
		super(
			threadsPerNode, threadsPerNode, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(
				threadsPerNode * runTimeConfig.getRunRequestQueueFactor()
			),
			new WorkerFactory(parentName + '<' + localReqConf.getAddr() + '>', context));
		//
		this.runTimeConfig = runTimeConfig;
		//
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
		//
		this.localReqConf = localReqConf;
		reqType = localReqConf.getLoadType();
		//
		counterSubm = parentMetrics.counter(MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_SUBM));
		counterSubmParent = parentMetrics.getCounters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_SUBM)
		);
		//
		counterRej = parentMetrics.counter(MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_REJ));
		counterRejParent = parentMetrics.getCounters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_REJ)
		);
		//
		counterReqSucc = parentMetrics.counter(MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_SUCC));
		counterReqSuccParent = parentMetrics.getCounters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_SUCC)
		);
		//
		counterReqFail = parentMetrics.counter(MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_FAIL));
		counterReqFailParent = parentMetrics.getCounters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_FAIL)
		);
		//
		reqDur = parentMetrics.histogram(
			MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_REQ, LoadExecutor.METRIC_NAME_DUR)
		);
		reqDurParent = parentMetrics.getHistograms().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_REQ, LoadExecutor.METRIC_NAME_DUR)
		);
		//
		reqBytes = parentMetrics.meter(
			MetricRegistry.name(toString(), LoadExecutor.METRIC_NAME_REQ, LoadExecutor.METRIC_NAME_BW)
		);
		reqBytesParent = parentMetrics.getMeters().get(
			MetricRegistry.name(parentName, LoadExecutor.METRIC_NAME_REQ, LoadExecutor.METRIC_NAME_BW)
		);
		//
	}
	//
	protected StorageNodeExecutorBase(
		final RunTimeConfig runTimeConfig,
		final String addr, final int threadsPerNode, final RequestConfig<T> sharedReqConf,
		final MetricRegistry parentMetrics, final String parentName, final Map<String,String> context
	) {
		this(
			runTimeConfig, threadsPerNode, sharedReqConf.clone().setAddr(addr),
			parentMetrics, parentName, context
		);
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		this.consumer = consumer;
	}
	//
	@Override
	public final Consumer<T> getConsumer() {
		return consumer;
	}
	//
	protected abstract Request<T> getRequestFor(final T dataItem);
	@Override @SuppressWarnings("unchecked")
	public final void submit(final T dataItem) {
		Request<T> request = null;
		try {
			request = getRequestFor(dataItem);
		} catch(final Exception e) {
			ExceptionHandler.trace(log, Level.DEBUG, e, "Failed to build request");
		} finally {
			log.trace(Markers.MSG, "Built request \"{}\"", request);
		}
		//
		boolean passed = false;
		int
			rejectCount = 0,
			retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec(),
			retryCountMax = runTimeConfig.getRunRetryCountMax();
		do {
			try {
				super.submit(request);
				passed = true;
			} catch(final RejectedExecutionException e) {
				rejectCount ++;
				try {
					Thread.sleep(rejectCount * retryDelayMilliSec);
				} catch(final InterruptedException ee) {
					break;
				}
			}
		} while(!passed && rejectCount < retryCountMax && !isShutdown());
		//
		if(dataItem != null) {
			if(passed) {
				counterSubm.inc();
				counterSubmParent.inc();
				if(log.isTraceEnabled(Markers.MSG)) {
					log.trace(
						Markers.MSG, "Request #{} for the object \"{}\" successfully submitted",
						counterSubmParent.getCount(), dataItem
					);
				}
			} else {
				counterRej.inc();
				counterRejParent.inc();
				if(log.isTraceEnabled(Markers.MSG)) {
					log.trace(
						Markers.MSG, "Request #{} for the object \"{}\" rejected",
						counterSubmParent.getCount(), dataItem
					);
				}
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final void afterExecute(final Runnable reqTask, final Throwable thrown) {
		if(thrown!=null) {
			log.warn(Markers.ERR, thrown.toString());
			counterReqFail.inc();
			counterReqFailParent.inc();
		} else {
			try(final Request<T> request = (Request<T>) Future.class.cast(reqTask).get()) {
				final T dataItem = request.getDataItem();
				if(dataItem == null) {
					consumer.submit(null);
				} else {
					final Request.Result result = request.getResult();
					if(result == Request.Result.SUCC) {
						// update the metrics with success
						counterReqSucc.inc();
						counterReqSuccParent.inc();
						final long
							duration = request.getDuration(),
							size = request.getTransferSize();
						reqBytes.mark(size);
						reqBytesParent.mark(size);
						reqDur.update(duration);
						reqDurParent.update(duration);
						// feed to the consumer
						if(consumer != null) {
							try {
								consumer.submit(dataItem);
							} catch(final IOException e) {
								ExceptionHandler.trace(
										log, Level.WARN, e,
									String.format(
										"Failed to submit the object \"%s\" to consumer", dataItem
									)
								);
							}
						}
					} else {
						counterReqFail.inc();
						counterReqFailParent.inc();
					}
				}
				//
			} catch(final InterruptedException e) {
				counterReqFail.inc();
				counterReqFailParent.inc();
				log.trace(Markers.ERR, "Interrupted while waiting for the response");
			} catch(final CancellationException e) {
				counterReqFail.inc();
				counterReqFailParent.inc();
				log.warn(Markers.ERR, "Request has been cancelled:", e);
			} catch(final ExecutionException e) {
				if(isShutdown()) {
					log.trace(Markers.ERR, "Request interrupted due to node executor shutdown");
					counterRej.inc();
					counterRejParent.inc();
				} else {
					final Throwable cause = e.getCause();
					if(InterruptedException.class.isInstance(cause)) {
						log.trace(Markers.MSG, "Poisoned");
						/*try {
							consumer.submit(null); // pass the poison through the consumer-producer chain
						} catch(final RemoteException ee) {
							log.debug(Markers.ERR, "Failed to feed the poison to consumer due to {}", ee.toString());
						}*/
					} else {
						ExceptionHandler.trace(
								log, Level.WARN, cause, "Unhandled request execution failure"
						);
						counterReqFail.inc();
						counterReqFailParent.inc();
					}
				}
			} catch(final Exception e) {
				counterReqFail.inc();
				counterReqFailParent.inc();
				log.warn(Markers.MSG, reqTask.getClass().getCanonicalName());
				ExceptionHandler.trace(log, Level.ERROR, e, "Unexpected failure");
			}
		}
		//
		super.afterExecute(reqTask, thrown);
	}
	//
	@Override
	public final String toString() {
		return getThreadFactory().toString();
	}
	//
	public final void logMetrics(final Level logLevel, final Marker logMarker) {
		final Snapshot reqDurSnapshot = reqDur.getSnapshot();
		final long
			countReqSucc = counterReqSucc.getCount(),
			countBytes = reqBytes.getCount();
		final double
			avgSize = countReqSucc==0 ? 0 : (double) countBytes / countReqSucc,
			meanBW = reqBytes.getMeanRate(),
			oneMinBW = reqBytes.getOneMinuteRate(),
			fiveMinBW = reqBytes.getFiveMinuteRate(),
			fifteenMinBW = reqBytes.getFifteenMinuteRate();
		final String message = LoadExecutor.MSG_FMT_METRICS.format(
				new Object[] {
						countReqSucc, getQueue().size() + getActiveCount(), counterReqFail.getCount(),
						//
						(float) reqDurSnapshot.getMin() / LoadExecutor.BILLION,
						(float) reqDurSnapshot.getMedian() / LoadExecutor.BILLION,
						(float) reqDurSnapshot.getMean() / LoadExecutor.BILLION,
						(float) reqDurSnapshot.getMax() / LoadExecutor.BILLION,
						//
						avgSize==0 ? 0 : meanBW/avgSize,
						avgSize==0 ? 0 : oneMinBW/avgSize,
						avgSize==0 ? 0 : fiveMinBW/avgSize,
						avgSize==0 ? 0 : fifteenMinBW/avgSize,
						//
						meanBW/LoadExecutor.MIB, oneMinBW/LoadExecutor.MIB,
						fiveMinBW/LoadExecutor.MIB, fifteenMinBW/LoadExecutor.MIB
				}
		);
		log.log(logLevel, logMarker, localReqConf.getAddr() + ": " + message);
		if(log.isTraceEnabled(Markers.PERF_AVG)) {
			log.trace(
				Markers.PERF_AVG,
				"{} internal metrics: shutdown: {}, terminated: {}, tasks: {} running, {} done, {} waiting",
				toString(), isShutdown(), isTerminated(), getActiveCount(), getCompletedTaskCount(),
				getQueue().size()
			);

		}
	}
	//
	@Override
	public final String getName() {
		return getThreadFactory().toString();
	}
	//
	@Override
	public final Producer<T> getProducer() {
		return null;
	}
	//
	@Override
	public final long getMaxCount()
		throws RemoteException {
		return consumer.getMaxCount();
	}
	//
	@Override
	public final void setMaxCount(final long maxCount) {
	}
	//
	public final String getAddr() {
		return localReqConf.getAddr();
	}
	//
	@Override
	public final void start() {
		prestartAllCoreThreads();
	}
	//
	@Override
	public final void join(final long milliSecs)
		throws InterruptedException {
		if(lock.tryLock()) {
			try {
				condInterrupted.await(milliSecs, TimeUnit.MILLISECONDS);
			} finally {
				lock.unlock();
			}
		}
	}
	//
	@Override
	public final void interrupt() {
		//
		log.debug(Markers.MSG, "Interrupting...");
		localReqConf.setRetries(false);
		//
		new GentleExecutorShutDown(this, runTimeConfig).run();
		//
		if(lock.tryLock()) {
			try {
				condInterrupted.signalAll();
			} finally {
				lock.unlock();
			}
		}
		//
	}
	//
	@Override
	public final boolean isShutdown() {
		return super.isShutdown()	||
			super.isTerminating()	||
			super.isTerminated();
	}
	//
	@Override
	public final void close()
		throws IOException {
		if(!isShutdown()) {
			interrupt();
		}
		//log.debug(Markers.MSG, "Dropping {} tasks", shutdownNow().size());
		synchronized(log) {
			log.debug(Markers.PERF_SUM, "Summary metrics below for {}", getName());
			logMetrics(Level.DEBUG, Markers.PERF_SUM);
		}
		//
		log.debug(Markers.MSG, "Closed {}", getThreadFactory().toString());
	}
	//
}
