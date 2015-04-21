package com.emc.mongoose.core.impl.load.executor;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 15.10.14.
 */
public abstract class LoadExecutorBase<T extends DataItem>
extends ThreadPoolExecutor
implements LoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final String name;
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
	private final AtomicLong
		durSumTasks = new AtomicLong(0),
		countTasksDone = new AtomicLong(0);
	private final Lock lock = new ReentrantLock();
	private final Condition condMaxCountReachedOrClosed = lock.newCondition();
	//
	protected LoadExecutorBase(
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias
	) {
		super(
			Producer.WORKER_COUNT, Producer.WORKER_COUNT, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(
				(maxCount > 0 && maxCount < runTimeConfig.getRunRequestQueueSize()) ?
					(int) maxCount : runTimeConfig.getRunRequestQueueSize()
			)
		);
		//
		final int loadNum = LAST_INSTANCE_NUM.getAndIncrement();
		storageNodeCount = addrs.length;
		name = Integer.toString(loadNum) + '-' +
			StringUtils.capitalize(reqConfig.getAPI().toLowerCase()) + '-' +
			StringUtils.capitalize(reqConfig.getLoadType().toString().toLowerCase()) +
			(maxCount > 0 ? Long.toString(maxCount) : "") + '-' +
			Integer.toString(connCountPerNode) + 'x' + Integer.toString(storageNodeCount);
		LOG.debug(
			LogUtil.MSG, "Determined queue capacity of {} for \"{}\"",
			getQueue().remainingCapacity(), name
		);
		//
		totalConnCount = connCountPerNode * storageNodeCount;
		//
		this.runTimeConfig = runTimeConfig;
		RequestConfig<T> reqConfigClone = null;
		try {
			reqConfigClone = reqConfig.clone();
		} catch(final CloneNotSupportedException e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Failed to clone the request config");
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
			LOG.debug(LogUtil.MSG, "{} will use file-based producer: {}", getName(), listFile);
		} else if(loadType == IOTask.Type.CREATE) {
			producer = newDataProducer(maxCount, sizeMin, sizeMax, sizeBias);
			LOG.debug(LogUtil.MSG, "{} will use new data items producer", getName());
		} else {
			producer = reqConfig.getAnyDataProducer(maxCount, addrs[0]);
			LOG.debug(LogUtil.MSG, "{} will use {} as data items producer", getName(), producer);
		}
		//
		if(producer != null) {
			try {
				producer.setConsumer(this);
			} catch(final RemoteException e) {
				LogUtil.failure(LOG, Level.WARN, e, "Unexpected failure");
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
		return name;
	}
	//
	@Override
	public final String toString() {
		return name;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Producer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final Thread metricDumpDaemon = new Thread() {
		//
		{ setDaemon(true); } // do not block process exit
		//
		@Override
		public final void run() {
			final long metricsUpdatePeriodMilliSec = TimeUnit.SECONDS.toMillis(
				runTimeConfig.getLoadMetricsPeriodSec()
			);
			try {
				if(metricsUpdatePeriodMilliSec > 0) {
					while(!isShutdown() || counterSubm.getCount() <= countTasksDone.get()) {
						logMetrics(LogUtil.PERF_AVG);
						Thread.sleep(metricsUpdatePeriodMilliSec);
					}
				} else {
					Thread.sleep(Long.MAX_VALUE);
				}
			} catch(final InterruptedException e) {
				LOG.debug(LogUtil.MSG, "Interrupted");
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
		final String message = LogUtil.PERF_SUM.equals(logMarker) ?
			String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_SUM_METRICS,
				//
				getName(),
				countReqSucc,
				countReqFail == 0 ?
					Long.toString(countReqFail) :
					(float) countReqSucc / countReqFail > 100 ?
						String.format(LogUtil.INT_YELLOW_OVER_GREEN, countReqFail) :
						String.format(LogUtil.INT_RED_OVER_GREEN, countReqFail),
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
				LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
				//
				countReqSucc, counterSubm.getCount() - countTasksDone.get(),
				countReqFail == 0 ?
					Long.toString(countReqFail) :
					(float) countReqSucc / countReqFail > 100 ?
						String.format(LogUtil.INT_YELLOW_OVER_GREEN, countReqFail) :
						String.format(LogUtil.INT_RED_OVER_GREEN, countReqFail),
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
			LOG.debug(LogUtil.MSG, "Starting {}", getName());
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
				LOG.debug(LogUtil.MSG, "{}: using an external data items producer", getName());
			} else {
				//
				try {
					producer.start();
					LOG.debug(LogUtil.MSG, "Started object producer {}", producer);
				} catch(final IOException e) {
					LogUtil.failure(LOG, Level.WARN, e, "Failed to start the producer");
				}
			}
			//
			jmxReporter.start();
			metricDumpDaemon.setName(getName());
			metricDumpDaemon.start();
			//
			LOG.debug(LogUtil.MSG, "Started \"{}\"", getName());
		} else {
			LOG.warn(LogUtil.ERR, "Second start attempt - skipped");
		}
	}
	//
	@Override
	public final void interrupt() {
		if(!isShutdown()) {
			shutdown();
		}
		LogUtil.trace(LOG, Level.DEBUG, LogUtil.MSG, String.format("Interrupted %s", getName()));
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
			LogUtil.MSG, "Appended the consumer \"{}\" for producer \"{}\"", consumer, getName()
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
							LogUtil.ERR,
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
	@Override
	public final void handleResult(final IOTask<T> ioTask, final IOTask.Status status) {
		final T dataItem = ioTask.getDataItem();
		final int latency = ioTask.getLatency();
		try {
			if(status == IOTask.Status.SUCC) {
				// update the metrics with success
				throughPut.mark();
				if(latency > 0) {
					respLatency.update(latency);
				}
				reqBytes.mark(ioTask.getTransferSize());
				durSumTasks.addAndGet(ioTask.getRespTimeDone() - ioTask.getReqTimeStart());
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "Task #{}: successfull result, {}/{}",
						ioTask.hashCode(), throughPut.getCount(), ioTask.getTransferSize()
					);
				}
				// feed to the consumer
				if(consumer != null) {
					if(LOG.isTraceEnabled(LogUtil.MSG)) {
						LOG.trace(
							LogUtil.MSG, "Going to feed the data item {} to the consumer {}",
							dataItem, consumer
						);
					}
					consumer.submit(dataItem);
					if(LOG.isTraceEnabled(LogUtil.MSG)) {
						LOG.trace(
							LogUtil.MSG, "The data item {} is passed to the consumer {} successfully",
							dataItem, consumer
						);
					}
				}
			} else if(!isClosed.get()) {
				counterReqFail.inc();
			}
		} catch(final InterruptedException e) {
			LOG.debug(LogUtil.MSG, "Interrupted");
		} catch(final RemoteException e) {
			LogUtil.failure(
				LOG, Level.WARN, e,
				String.format("Failed to submit the data item \"%s\" to \"%s\"", dataItem, consumer)
			);
		} catch(final RejectedExecutionException e) {
			LogUtil.failure(
				LOG, Level.DEBUG, e,
				String.format("\"%s\" rejected the data item \"%s\"", consumer, dataItem)
			);
		} finally {
			final long n = countTasksDone.incrementAndGet();
			if(isShutdown() && n >= counterSubm.getCount()) {
				LOG.debug(LogUtil.MSG, "Done condition is met at {} processed items", n);
				try {
					if(lock.tryLock(1, TimeUnit.SECONDS)) {
						condMaxCountReachedOrClosed.signalAll();
						lock.unlock();
					} else {
						LOG.warn(LogUtil.ERR, "Failed to acquire the lock for result handling");
					}
				} catch(final InterruptedException e) {
					LogUtil.failure(LOG, Level.WARN, e, "Interrupted");
				}
			}
		}
	}
	//
	@Override
	public final synchronized void shutdown() {
		if(producer != null) {
			// stop the producing
			try {
				producer.interrupt();
				LOG.debug(
					LogUtil.MSG, "Stopped the producer \"{}\" for \"{}\"", producer, getName()
				);
			} catch(final IOException e) {
				LogUtil.failure(
					LOG, Level.WARN, e,
					String.format("Failed to stop the producer: %s", producer.toString())
				);
			}
		}
		//
		if(!isShutdown()) {
			super.shutdown();
			LOG.debug(LogUtil.MSG, "\"{}\" will not accept new tasks more", getName());
		}
	}
	//
	private final AtomicBoolean isClosed = new AtomicBoolean(false);
	//
	@Override
	public void close()
	throws IOException {
		LOG.debug(LogUtil.MSG, "Invoked close for {}", getName());
		if(isClosed.compareAndSet(false, true)) {
			final long tsStartNanoSec = tsStart.get();
			if(tsStartNanoSec > 0) {
				interrupt();
				metricDumpDaemon.interrupt();
				logMetrics(LogUtil.PERF_SUM); // provide summary metrics
				// calculate the efficiency and report
				final float
					loadDurMicroSec = (float) (System.nanoTime() - tsStart.get()) / 1000,
					eff = durSumTasks.get() / (loadDurMicroSec * totalConnCount);
				LOG.debug(
					LogUtil.PERF_SUM,
					String.format(
						LogUtil.LOCALE_DEFAULT,
						"Load execution duration: %3.3f[sec], efficiency estimation: %3.3f[%%]",
						loadDurMicroSec / 1e6, 100 * eff
					)
				);
			}
			//
			try {
				if(lock.tryLock(runTimeConfig.getRunReqTimeOutMilliSec(), TimeUnit.MILLISECONDS)) {
					condMaxCountReachedOrClosed.signalAll();
				} else {
					LOG.warn(LogUtil.ERR, "Failed to acquire the lock in close method");
				}
			} catch(final InterruptedException e) {
				LogUtil.failure(LOG, Level.WARN, e, "Interrupted while acquiring the lock");
			}
			//
			try {
				LOG.debug(LogUtil.MSG, "Forcing the shutdown");
				reqConfigCopy.close(); // disables connection drop failures
				LOG.debug(LogUtil.MSG, "{}: dropped {} tasks", getName(), shutdownNow().size());
				// poison the consumer
				consumer.shutdown();
				LOG.debug(LogUtil.MSG, "Consumer \"{}\" has been poisoned", consumer);
			} catch(final IllegalStateException | RejectedExecutionException e) {
				LogUtil.failure(LOG, Level.DEBUG, e, "Failed to poison the consumer");
			} finally {
				jmxReporter.close();
				LOG.debug(LogUtil.MSG, "JMX reported closed");
				LoadCloseHook.del(this);
				LOG.debug(LogUtil.MSG, "\"{}\" closed successfully", getName());
			}
		} else {
			LOG.debug(
				LogUtil.MSG,
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
			LogUtil.failure(
				LOG, Level.WARN, e, String.format("%s: failed to close", getName())
			);
		} finally {
			super.finalize();
		}
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
	throws RemoteException {
		join(Long.MAX_VALUE);
	}
	//
	@Override
	public final void join(final long timeOutMilliSec)
	throws RemoteException {
		long t = System.currentTimeMillis();
		try {
			if(lock.tryLock(timeOutMilliSec, TimeUnit.MILLISECONDS)) {
				t = System.currentTimeMillis() - t; // the count of time wasted for locking
				LOG.debug(
					LogUtil.MSG, "Join: wait for the done condition at most for {}[ms]",
					timeOutMilliSec - t
				);
				if(condMaxCountReachedOrClosed.await(timeOutMilliSec - t, TimeUnit.MILLISECONDS)) {
					LOG.debug(LogUtil.MSG, "{}: join finished", getName());
				} else {
					LOG.debug(
						LogUtil.MSG, "{}: join timeout, tasks left: {} enqueued, {} active",
						getName(), getQueue().size(), getActiveCount()
					);
				}
			} else {
				LOG.warn(LogUtil.ERR, "Failed to acquire the lock for the join method");
			}
		} catch(final InterruptedException e) {
			LogUtil.failure(LOG, Level.DEBUG, e, String.format("%s: join interrupted", getName()));
		} finally {
			lock.unlock();
		}
	}
}
