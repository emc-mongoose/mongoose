package com.emc.mongoose.core.impl.load.executor;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
//
import com.emc.mongoose.common.logging.Settings;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.ConsoleColors;
import com.emc.mongoose.common.logging.TraceLogger;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.src.DataSource;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.Producer;
//
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
import com.emc.mongoose.core.impl.load.tasks.RequestResultTask;
import com.emc.mongoose.core.impl.load.model.LogConsumer;
import com.emc.mongoose.core.impl.load.executor.util.DataObjectWorkerFactory;
//
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import javax.management.MBeanServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 15.10.14.
 */
public abstract class LoadExecutorBase<T extends DataItem>
extends ThreadPoolExecutor
implements LoadExecutor<T> {
	//
	private final Logger LOG = LogManager.getLogger();
	//
	protected final int connCountPerNode, storageNodeCount, retryCountMax, retryDelayMilliSec;
	protected final String storageNodeAddrs[];
	//
	protected final DataSource dataSrc;
	protected volatile RunTimeConfig runTimeConfig = RunTimeConfig.getContext();
	protected final RequestConfig<T> reqConfigCopy;
	protected final IOTask.Type loadType;
	//
	protected volatile Producer<T> producer = null;
	protected volatile Consumer<T> consumer;
	private final long maxCount;
	private final int totalConnCount;
	// METRICS section BEGIN
	protected final MetricRegistry metrics = new MetricRegistry();
	protected Counter counterSubm, counterRej, counterReqFail;
	protected Meter throughPut, reqBytes;
	protected Histogram respLatency;
	//
	protected final MBeanServer mBeanServer;
	protected final JmxReporter jmxReporter;
	// METRICS section END
	protected LoadExecutorBase(
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final int queueSize
	) {
		super(
			1, 1, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(queueSize)
		);
		//
		final int loadNum = LAST_INSTANCE_NUM.getAndIncrement();
		storageNodeCount = addrs.length;
		final String name = Integer.toString(loadNum) + '-' +
			StringUtils.capitalize(reqConfig.getAPI().toLowerCase()) + '-' +
			StringUtils.capitalize(reqConfig.getLoadType().toString().toLowerCase()) +
			(maxCount > 0 ? Long.toString(maxCount) : "") + '-' +
			Integer.toString(connCountPerNode) + 'x' + Integer.toString(storageNodeCount);
		LOG.debug(
			Markers.MSG, "Determined queue capacity of {} for \"{}\"",
			getQueue().remainingCapacity(), name
		);
		//
		totalConnCount = connCountPerNode * storageNodeCount;
		setCorePoolSize(Math.min(COUNT_THREADS_MIN, storageNodeCount));
		setMaximumPoolSize(getCorePoolSize());
		//
		this.runTimeConfig = runTimeConfig;
		RequestConfig<T> reqConfigClone = null;
		try {
			reqConfigClone = reqConfig.clone();
		} catch(final CloneNotSupportedException e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "Failed to clone the request config");
		} finally {
			this.reqConfigCopy = reqConfigClone;
		}
		loadType = reqConfig.getLoadType();
		//
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		mBeanServer = ServiceUtils.getMBeanServer(runTimeConfig.getRemotePortExport());
		jmxReporter = JmxReporter.forRegistry(metrics)
			//.convertDurationsTo(TimeUnit.MICROSECONDS)
			//.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		//
		setThreadFactory(
			new DataObjectWorkerFactory(loadNum, reqConfig.getAPI(), loadType, name)
		);
		this.connCountPerNode = connCountPerNode;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		// prepare the node executors array
		storageNodeAddrs = addrs.clone();
		// create and configure the connection manager
		dataSrc = reqConfig.getDataSource();
		//
		if(listFile != null && listFile.length() > 0 && Files.isReadable(Paths.get(listFile))) {
			producer = newFileBasedProducer(maxCount, listFile);
			LOG.debug(Markers.MSG, "{} will use file-based producer: {}", getName(), listFile);
		} else if(loadType == IOTask.Type.CREATE) {
			producer = newDataProducer(maxCount, sizeMin, sizeMax, sizeBias);
			LOG.debug(Markers.MSG, "{} will use new data items producer", getName());
		} else {
			producer = reqConfig.getAnyDataProducer(maxCount, addrs[0]);
			LOG.debug(Markers.MSG, "{} will use {} as data items producer", getName(), producer);
		}
		//
		if(producer != null) {
			try {
				producer.setConsumer(this);
			} catch(final RemoteException e) {
				TraceLogger.failure(LOG, Level.WARN, e, "Unexpected failure");
			}
		}
		setConsumer(new LogConsumer<T>(maxCount, getCorePoolSize())); // by default, may be overriden later externally
		LoadCloseHook.add(this);
	}
	//
	protected abstract Producer<T> newFileBasedProducer(final long maxCount, final String listFile);
	protected abstract Producer<T> newDataProducer(
		final long maxCount, final long minObjSize, final long maxObjSize, final float objSizeBias
	);
	//
	@Override
	public final String getName() {
		return getThreadFactory().toString();
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Producer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final Thread metricDumpThread = new Thread() {
		@Override
		public final void run() {
			final int metricsUpdatePeriodSec = runTimeConfig.getLoadMetricsPeriodSec();
			try {
				if(metricsUpdatePeriodSec > 0) {
					while(isAlive()) {
						logMetrics(Markers.PERF_AVG);
						Thread.sleep(metricsUpdatePeriodSec * 1000);
					}
				} else {
					Thread.sleep(Long.MAX_VALUE);
				}
			} catch(final InterruptedException e) {
				LOG.debug(Markers.MSG, "Interrupted");
			}
		}
	};
	//
	@Override
	public final void logMetrics(final Marker logMarker) {
		//
		final long
			countReqSucc = throughPut.getCount(),
			countReqFail = counterReqFail.getCount();
		final double
			meanTP = throughPut.getMeanRate(),
			oneMinTP = throughPut.getOneMinuteRate(),
			fiveMinTP = throughPut.getFiveMinuteRate(),
			fifteenMinTP = throughPut.getFifteenMinuteRate(),
			meanBW = reqBytes.getMeanRate(),
			oneMinBW = reqBytes.getOneMinuteRate(),
			fiveMinBW = reqBytes.getFiveMinuteRate(),
			fifteenMinBW = reqBytes.getFifteenMinuteRate();
		final Snapshot respLatencySnapshot = respLatency.getSnapshot();
		//
		final String message = Markers.PERF_SUM.equals(logMarker) ?
			String.format(
				Settings.LOCALE_DEFAULT, MSG_FMT_SUM_METRICS,
				//
				getName(),
				countReqSucc,
				countReqFail == 0 ?
					Long.toString(countReqFail) :
					(float) countReqSucc / countReqFail > 100 ?
						String.format(ConsoleColors.INT_YELLOW_OVER_GREEN, countReqFail) :
						String.format(ConsoleColors.INT_RED_OVER_GREEN, countReqFail),
				//
				(int) respLatencySnapshot.getMean(),
				(int) respLatencySnapshot.getMin(),
				(int) respLatencySnapshot.getMedian(),
				(int) respLatencySnapshot.getMax(),
				//
				meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
				meanBW / MIB, oneMinBW / MIB, fiveMinBW / MIB, fifteenMinBW / MIB
			) :
			String.format(
				Settings.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				countReqSucc, throughPut.getCount() - countReqSucc,
				countReqFail == 0 ?
					Long.toString(countReqFail) :
					(float) countReqSucc / countReqFail > 100 ?
						String.format(ConsoleColors.INT_YELLOW_OVER_GREEN, countReqFail) :
						String.format(ConsoleColors.INT_RED_OVER_GREEN, countReqFail),
				//
				(int) respLatencySnapshot.getMean(),
				(int) respLatencySnapshot.getMin(),
				(int) respLatencySnapshot.getMedian(),
				(int) respLatencySnapshot.getMax(),
				//
				meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
				meanBW / MIB, oneMinBW / MIB, fiveMinBW / MIB, fifteenMinBW / MIB
			);
		LOG.info(logMarker, message);
		/*
		if(Markers.PERF_SUM.equals(logMarker)) {
			final double totalReqNanoSeconds = reqDurSnapshot.getMean() * countReqSucc;
			LOG.debug(
				Markers.PERF_SUM,
				String.format(
					Main.LOCALE_DEFAULT, FMT_EFF_SUM,
					100 * totalReqNanoSeconds / ((System.nanoTime() - tsStart) * getTotalConnCount())
				)
			);
		}
		//
		if(LOG.isTraceEnabled(Markers.PERF_AVG)) {
			for(final StorageNodeExecutor<T> node: storageNodeAddrs) {
				node.logMetrics(Level.TRACE, Markers.PERF_AVG);
			}
		}*/
		//
	}
	//
	private final AtomicLong tsStart = new AtomicLong(-1);
	//
	@Override
	public void start() {
		if(tsStart.compareAndSet(-1, System.nanoTime())) {
			LOG.debug(Markers.MSG, "Starting {}", getName());
			//
			prestartAllCoreThreads();
			//
			final String name = getName();
			// init metrics
			counterSubm = metrics.counter(MetricRegistry.name(name, METRIC_NAME_SUBM));
			counterRej = metrics.counter(MetricRegistry.name(name, METRIC_NAME_REJ));
			counterReqFail = metrics.counter(MetricRegistry.name(name, METRIC_NAME_FAIL));
			throughPut = metrics.meter(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_TP));
			reqBytes = metrics.meter(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_BW));
			//reqDur = metrics.histogram(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_DUR));
			respLatency = metrics.histogram(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_LAT));
			//
			if(producer == null) {
				LOG.debug(Markers.MSG, "{}: using an external data items producer", getName());
			} else {
				//
				try {
					producer.start();
					LOG.debug(Markers.MSG, "Started object producer {}", producer);
				} catch(final IOException e) {
					TraceLogger.failure(LOG, Level.WARN, e, "Failed to start the producer");
				}
			}
			//
			jmxReporter.start();
			metricDumpThread.setName(getName());
			metricDumpThread.start();
			//
			LOG.debug(Markers.MSG, "Started \"{}\"", getName());
		} else {
			LOG.warn(Markers.ERR, "Second start attempt - skipped");
		}
	}
	//
	@Override
	public final void interrupt() {
		if(!isShutdown()) {
			shutdown();
		}
		if(metricDumpThread.isAlive()) {
			LOG.debug(Markers.MSG, "{}: interrupting...", getName());
			metricDumpThread.interrupt();
		}
	}
	//
	@Override
	public final Consumer<T> getConsumer() {
		return consumer;
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		this.consumer = consumer;
		LOG.debug(
			Markers.MSG, "Appended the consumer \"{}\" for producer \"{}\"", consumer, getName()
		);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected final AtomicLong countSubmCalls = new AtomicLong(0);
	//
	@Override @SuppressWarnings("unchecked")
	public void submit(final T dataItem)
	throws RemoteException, RejectedExecutionException, InterruptedException {
		if(tsStart.get() < 0) {
			throw new RejectedExecutionException(
				"Not started yet, rejecting the submit of the data item"
			);
		} else if(maxCount > counterSubm.getCount()) {
			if(dataItem == null) {
				LOG.debug(Markers.MSG, "{}: poison submitted, performing the shutdown", getName());
				shutdown(); // stop further submitting
			} else {
				// round-robin node selection
				final String tgtNodeAddr = storageNodeAddrs[
					(int) (countSubmCalls.getAndIncrement() % storageNodeCount)
				];
				// prepare the I/O task instance (make the link between the data item and load type)
				final IOTask<T> ioTask = reqConfigCopy.getRequestFor(dataItem, tgtNodeAddr);
				// submit the corresponding I/O task
				final Future<IOTask.Status> futureResponse = submit(ioTask);
				// prepare the corresponding result handling task
				final RequestResultTask<T>
					handleResultTask = (RequestResultTask<T>) RequestResultTask.getInstance(
						this, ioTask, futureResponse
					);
				Future futureResult = null;
				int rejectCount = 0;
				do {
					try { // submit the result handling task
						futureResult = submit(handleResultTask);
						counterSubm.inc();
					} catch(final RejectedExecutionException e) {
						rejectCount ++;
						try {
							Thread.sleep(rejectCount * retryDelayMilliSec);
						} catch(final InterruptedException ee) {
							LOG.trace(
								Markers.ERR,
								"Got interruption, won't submit result handling for task #{}",
								ioTask.hashCode()
							);
						}
					}
					//
				} while(futureResult == null && rejectCount < retryCountMax && !isShutdown());
				//
				if(futureResponse == null) {
					counterRej.inc();
				}
			}
		} else {
			shutdown();
		}
		//
		if(isShutdown()) {
			throw new InterruptedException(
				String.format(
					"%s: max data item count (%d) have been submitted, shutdown the submit executor",
					getName(), maxCount
				)
			);
		}
	}
	//
	private final AtomicLong durSumTasks = new AtomicLong(0);
	//
	@Override
	public final void handleResult(final IOTask<T> ioTask, final IOTask.Status status) {
		final T dataItem = ioTask.getDataItem();
		final int latency = ioTask.getLatency();
		try {
			if(dataItem == null) {
				consumer.submit(null);
			} else if(status == IOTask.Status.SUCC) {
				// update the metrics with success
				throughPut.mark();
				if(latency > 0) {
					respLatency.update(latency);
				}
				reqBytes.mark(ioTask.getTransferSize());
				durSumTasks.addAndGet(ioTask.getRespTimeDone() - ioTask.getReqTimeStart());
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "Task #{}: successfull result, {}/{}",
						ioTask.hashCode(), throughPut.getCount(), ioTask.getTransferSize()
					);
				}
				// feed to the consumer
				if(consumer != null) {
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Going to feed the data item {} to the consumer {}",
							dataItem, consumer
						);
					}
					consumer.submit(dataItem);
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "The data item {} is passed to the consumer {} successfully",
							dataItem, consumer
						);
					}
				}
			} else if(!isClosed.get()) {
				counterReqFail.inc();
			}
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		} catch(final RemoteException e) {
			TraceLogger.failure(
				LOG, Level.WARN, e,
				String.format("Failed to submit the data item \"%s\" to \"%s\"", dataItem, consumer)
			);
		} catch(final RejectedExecutionException e) {
			TraceLogger.failure(
				LOG, Level.WARN, e,
				String.format("\"%s\" rejected the data item \"%s\"", consumer, dataItem)
			);
		}
	}
	//
	@Override
	public final synchronized void shutdown() {
		if(producer != null) {
			// stop the producing
			try {
				producer.interrupt();
			} catch(final IOException e) {
				TraceLogger.failure(
					LOG, Level.WARN, e,
					String.format("Failed to stop the producer: %s", producer.toString())
				);
			}
		}
		//
		if(!isShutdown()) {
			super.shutdown(); // stop the submitting
		}
	}
	//
	private final AtomicBoolean isClosed = new AtomicBoolean(false);
	//
	@Override
	public void close()
	throws IOException {
		TraceLogger.trace(
			LOG, Level.TRACE, Markers.MSG, String.format("invoked close of %s", getName())
		);
		if(isClosed.compareAndSet(false, true)) {
			final long tsStartNanoSec = tsStart.get();
			if(tsStartNanoSec > 0) {
				interrupt();
				logMetrics(Markers.PERF_SUM); // provide summary metrics
				// calculate the efficiency and report
				final float
					loadDurMicroSec = (float) (System.nanoTime() - tsStart.get()) / 1000,
					eff = durSumTasks.get() / (loadDurMicroSec * totalConnCount);
				LOG.debug(
					Markers.PERF_SUM,
					String.format(
						Settings.LOCALE_DEFAULT,
						"Load execution duration: %3.3f[sec], efficiency estimation: %3.3f[%%]",
						loadDurMicroSec / 1e6, 100 * eff
					)
				);
			}
			try {
				// force shutdown
				reqConfigCopy.close(); // disables connection drop failures
				LOG.debug(Markers.MSG, "{}: dropped {} tasks", getName(), shutdownNow().size());
				// poison the consumer
				consumer.submit(null);
			} catch(final InterruptedException e) {
				TraceLogger.failure(
					LOG, Level.TRACE, e,
					String.format(
						"%s: interrupted on feeding the poison to the consumer", getName()
					)
				);
			} catch(final IllegalStateException | RejectedExecutionException e) {
				TraceLogger.failure(LOG, Level.DEBUG, e, "Failed to poison the consumer");
			} finally {
				jmxReporter.close();
				LoadCloseHook.del(this);
				LOG.debug(Markers.MSG, "\"{}\" closed successfully", getName());
			}
		} else {
			LOG.debug(
				Markers.MSG,
				"Not closing \"{}\" because it has been closed before already", getName()
			);
		}
	}
	//
	@Override
	protected final void finalize() {
		try {
			close();
		} catch(final IOException e) {
			TraceLogger.failure(
				LOG, Level.WARN, e, String.format("%s: failed to close", getName())
			);
		}
		super.finalize();
	}
	//
	@Override
	public final Producer<T> getProducer() {
		return producer;
	}
	//
	@Override
	public final long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public final void join()
	throws InterruptedException {
		LOG.trace(
			Markers.MSG, "{}: waiting remaining {} tasks to complete",
			getName(), getQueue().size() + getActiveCount()
		);
		awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		LOG.trace(Markers.MSG, "{} interrupted and done", getName());
	}
	//
	@Override
	public final void join(final long timeOutMilliSec)
	throws InterruptedException {
		LOG.trace(
			Markers.MSG, "{}: waiting remaining {} tasks to complete",
			getName(), getQueue().size() + getActiveCount()
		);
		awaitTermination(timeOutMilliSec, TimeUnit.MILLISECONDS);
		LOG.trace(Markers.MSG, "{} interrupted and done", getName());
	}
}
