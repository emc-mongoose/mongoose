package com.emc.mongoose.base.load.impl;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.base.data.persist.LogConsumer;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.base.load.impl.tasks.LoadCloseHook;
import com.emc.mongoose.base.load.impl.tasks.RequestResultTask;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.util.threading.DataObjectWorkerFactory;
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
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 15.10.14.
 */
public abstract class LoadExecutorBase<T extends DataItem>
extends ThreadPoolExecutor
implements LoadExecutor<T> {
	//
	private final Logger LOG = LogManager.getLogger();
	//
	private final String name;
	protected final int threadsPerNode, storageNodeCount, retryCountMax, retryDelayMilliSec;
	protected final String storageNodeAddrs[];
	//
	protected final DataSource<T> dataSrc;
	protected volatile RunTimeConfig runTimeConfig = Main.RUN_TIME_CONFIG.get();
	protected final RequestConfig<T> reqConfig;
	protected final AsyncIOTask.Type loadType;
	//
	protected volatile Producer<T> producer = null;
	protected volatile Consumer<T> consumer;
	private final long maxCount;
	// METRICS section BEGIN
	protected final MetricRegistry metrics = new MetricRegistry();
	protected final Counter counterSubm, counterRej, counterReqSucc, counterReqFail;
	protected final Meter reqBytes;
	protected Histogram respLatency;
	//
	protected final MBeanServer mBeanServer;
	//
	protected final JmxReporter jmxReporter;
	// METRICS section END
	private final static AtomicInteger LAST_INSTANCE_NUM = new AtomicInteger(0);
	//
	protected LoadExecutorBase(
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int threadsPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias
	) {
		super(
			1, 1, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(runTimeConfig.getRunRequestQueueSize())
		);
		//
		storageNodeCount = addrs.length;
		final int totalThreadCount = threadsPerNode * storageNodeCount;
		setCorePoolSize(totalThreadCount);
		setMaximumPoolSize(totalThreadCount);
		//
		this.runTimeConfig = runTimeConfig;
		this.reqConfig = reqConfig;
		loadType = reqConfig.getLoadType();
		final int loadNum = LAST_INSTANCE_NUM.getAndIncrement();
		setThreadFactory(
			new DataObjectWorkerFactory("loadWorker", loadNum, reqConfig.getAPI(), loadType)
		);
		//
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		mBeanServer = ServiceUtils.getMBeanServer(runTimeConfig.getRemoteExportPort());
		jmxReporter = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		//
		name = Integer.toString(loadNum) + '-' +
			StringUtils.capitalize(reqConfig.getAPI().toLowerCase()) + '-' +
			StringUtils.capitalize(reqConfig.getLoadType().toString().toLowerCase()) +
			(maxCount > 0? Long.toString(maxCount) : "") + '-' +
			Integer.toString(threadsPerNode) + 'x' + Integer.toString(storageNodeCount);
		this.threadsPerNode = threadsPerNode;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		// init metrics
		counterSubm = metrics.counter(MetricRegistry.name(name, METRIC_NAME_SUBM));
		counterRej = metrics.counter(MetricRegistry.name(name, METRIC_NAME_REJ));
		counterReqSucc = metrics.counter(MetricRegistry.name(name, METRIC_NAME_SUCC));
		counterReqFail = metrics.counter(MetricRegistry.name(name, METRIC_NAME_FAIL));
		reqBytes = metrics.meter(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_BW));
		//reqDur = metrics.histogram(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_DUR));
		respLatency = metrics.histogram(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_LAT));
		jmxReporter.start();
		// prepare the node executors array
		storageNodeAddrs = addrs.clone();
		// create and configure the connection manager
		dataSrc = reqConfig.getDataSource();
		//
		if(listFile != null && listFile.length() > 0 && Files.isReadable(Paths.get(listFile))) {
			producer = newFileBasedProducer(maxCount, listFile);
			LOG.debug(Markers.MSG, "{} will use file-based producer: {}", getName(), listFile);
		} else if(loadType == AsyncIOTask.Type.CREATE) {
			producer = newDataProducer(maxCount, sizeMin, sizeMax, sizeBias);
			LOG.debug(Markers.MSG, "{} will use new data items producer", getName());
		} else {
			producer = reqConfig.getAnyDataProducer(maxCount, this);
			LOG.debug(Markers.MSG, "{} will use {} as data items producer", getName(), producer);
		}
		//
		if(producer != null) {
			try {
				producer.setConsumer(this);
			} catch(final RemoteException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Unexpected failure");
			}
		}
		setConsumer(new LogConsumer<T>()); // by default, may be overriden later externally
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
		return name;
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Producer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final Thread metricDumpThread = new Thread(getName() + "-dumpMetrics") {
		@Override
		public final void run() {
			final int metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
			if(metricsUpdatePeriodSec > 0) {
				while(isAlive()) {
					logMetrics(Markers.PERF_AVG);
				}
			} else {
				try {
					Thread.sleep(Long.MAX_VALUE);
				} catch(final InterruptedException e) {
					LOG.debug(Markers.MSG, "Interrupted");
				}
			}
		}
	};
	//
	protected final void logMetrics(final Marker logMarker) {
		//
		final long
			countReqSucc = counterReqSucc.getCount(),
			countBytes = reqBytes.getCount();
		final double
			avgSize = countReqSucc==0 ? 0 : (double) countBytes / countReqSucc,
			meanBW = reqBytes.getMeanRate(),
			oneMinBW = reqBytes.getOneMinuteRate(),
			fiveMinBW = reqBytes.getFiveMinuteRate(),
			fifteenMinBW = reqBytes.getFifteenMinuteRate();
		final Snapshot respLatencySnapshot = respLatency.getSnapshot();
		//
		final int notCompletedTaskCount = getQueue().size() + getActiveCount();
		//
		final String message = Markers.PERF_SUM.equals(logMarker) ?
			String.format(
				Main.LOCALE_DEFAULT, MSG_FMT_SUM_METRICS,
				//
				getName(),
				countReqSucc, counterReqFail.getCount(),
				//
				(float) respLatencySnapshot.getMean() / BILLION,
				(float) respLatencySnapshot.getMin() / BILLION,
				(float) respLatencySnapshot.getMedian() / BILLION,
				(float) respLatencySnapshot.getMax() / BILLION,
				//
				avgSize==0 ? 0 : meanBW / avgSize,
				avgSize==0 ? 0 : oneMinBW / avgSize,
				avgSize==0 ? 0 : fiveMinBW / avgSize,
				avgSize==0 ? 0 : fifteenMinBW / avgSize,
				//
				meanBW / MIB,
				oneMinBW / MIB,
				fiveMinBW / MIB,
				fifteenMinBW / MIB
			) :
			String.format(
				Main.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				countReqSucc, notCompletedTaskCount, counterReqFail.getCount(),
				//
				(float) respLatencySnapshot.getMean() / BILLION,
				(float) respLatencySnapshot.getMin() / BILLION,
				(float) respLatencySnapshot.getMedian() / BILLION,
				(float) respLatencySnapshot.getMax() / BILLION,
				//
				avgSize==0 ? 0 : meanBW / avgSize,
				avgSize==0 ? 0 : oneMinBW / avgSize,
				avgSize==0 ? 0 : fiveMinBW / avgSize,
				avgSize==0 ? 0 : fifteenMinBW / avgSize,
				//
				meanBW / MIB,
				oneMinBW / MIB,
				fiveMinBW / MIB,
				fifteenMinBW / MIB
			);
		LOG.info(logMarker, message);
		/*
		if(Markers.PERF_SUM.equals(logMarker)) {
			final double totalReqNanoSeconds = reqDurSnapshot.getMean() * countReqSucc;
			LOG.debug(
				Markers.PERF_SUM,
				String.format(
					Main.LOCALE_DEFAULT, FMT_EFF_SUM,
					100 * totalReqNanoSeconds / ((System.nanoTime() - tsStart) * getThreadCount())
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
	@Override
	public void start() {
		if(metricDumpThread.isAlive()) {
			LOG.warn(Markers.ERR, "Second start attempt - skipped");
		} else {
			//
			reqConfig.configureStorage(this);
			prestartAllCoreThreads();
			//
			if(producer==null) {
				LOG.debug(Markers.MSG, "{}: using an external data items producer", getName());
			} else {
				try {
					producer.start();
					LOG.debug(
						Markers.MSG, "Started object producer {}",
						producer.getClass().getSimpleName()
					);
				} catch(final IOException e) {
					ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to start the producer");
				}
			}
			//
			metricDumpThread.start();
			//
			LOG.info(Markers.MSG, "Started \"{}\"", getName());
		}
	}
	//
	@Override
	public final void interrupt() {
		if(!isShutdown()) {
			shutdown();
		}
		if(metricDumpThread.isAlive()) {
			LOG.debug(Markers.MSG, "{}: interrupting...", name);
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
	@Override
	public void submit(final T dataItem)
	throws RemoteException, InterruptedException {
		if(maxCount > getTaskCount()) {
			if(dataItem == null) {
				LOG.debug(Markers.MSG, "{}: poison submitted, performing the shutdown", name);
				shutdown(); // stop further submitting
			} else {
				// round-robin node selection
				final String tgtNodeAddr = storageNodeAddrs[
					(int) getTaskCount() % storageNodeCount
				];
				// prepare the I/O task instance (make the link between the data item and load type)
				final AsyncIOTask<T> ioTask = reqConfig.getRequestFor(dataItem, tgtNodeAddr);
				// submit the corresponding I/O task
				final Future<AsyncIOTask.Result> futureResponse = submit(ioTask);
				// prepare the corresponding result handling task
				final RequestResultTask<T> handleResultTask = new RequestResultTask<>(
					this, ioTask, futureResponse
				);
				Future futureResult = null;
				int rejectCount = 0;
				do {
					try { // submit the result handling task
						futureResult = submit(handleResultTask);
					} catch(final RejectedExecutionException e) {
						rejectCount ++;
						try {
							Thread.sleep(rejectCount * retryDelayMilliSec);
						} catch(final InterruptedException ee) {
							break;
						}
					}
					//
				} while(futureResult == null && rejectCount < retryCountMax && !isShutdown());
				//
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						futureResponse == null ? Markers.ERR : Markers.MSG,
						"Data item \"{}\" submit {} after {} retries", dataItem,
						futureResponse == null ? "failure" : "success", rejectCount
					);
				}
			}
		} else {
			shutdown();
			throw new InterruptedException(
				String.format(
					"%s: max data item count (%d) have been submitted, shutdown the submit executor",
					getName(), maxCount
				)
			);
		}
	}
	//
	@Override
	public final void handleResult(final AsyncIOTask<T> ioTask, final AsyncIOTask.Result result) {
		final T dataItem = ioTask.getDataItem();
		try {
			if(dataItem == null) {
				consumer.submit(null);
			} else if(result == AsyncIOTask.Result.SUCC) {
				// update the metrics with success
				counterReqSucc.inc();
				final long
					latency = ioTask.getRespTimeStart() - ioTask.getReqTimeDone(),
					size = ioTask.getTransferSize();
				reqBytes.mark(size);
				//reqDur.update(duration);
				//reqDurParent.update(duration);
				respLatency.update(latency);
				// feed to the consumer
				if(consumer != null) {
					consumer.submit(dataItem);
				}
			} else {
				counterReqFail.inc();
			}
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		} catch(final RemoteException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Looks like a network failure");
		}
	}
	//
	@Override
	public final synchronized void shutdown() {
		// stop the producing
		try {
			producer.interrupt();
		} catch(final IOException e) {
			ExceptionHandler.trace(
				LOG, Level.WARN, e,
				String.format("Failed to stop the producer: %s", producer.toString())
			);
		}// stop the submitting
		super.shutdown();
	}
	//
	@Override
	public synchronized void close() {
		try {
			interrupt();
			// provide summary metrics
			logMetrics(Markers.PERF_SUM);
			//
			final int reqTimeOutMilliSec = runTimeConfig.getRunReqTimeOutMilliSec();
			awaitTermination(reqTimeOutMilliSec, TimeUnit.MILLISECONDS);
			LOG.debug(Markers.MSG, "{}: waiting the remaining tasks done", name);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.ERR, "{}: closing interrupted", name);
		} finally {
			try {
				// force shutdown
				LOG.debug(Markers.MSG, "{}: dropped {} tasks", name, shutdownNow().size());
				// poison the consumer
				consumer.submit(null);
			} catch(final RemoteException | InterruptedException e) {
				ExceptionHandler.trace(LOG, Level.DEBUG, e, "Failed to feed the poison");
			} finally {
				// close node executors
				jmxReporter.close();
				//
				LoadCloseHook.del(this);
			}
		}
	}
	//
	@Override
	protected final void finalize() {
		close();
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
}
