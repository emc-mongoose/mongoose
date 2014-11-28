package com.emc.mongoose.base.load.impl;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.base.data.persist.LogConsumer;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.base.load.StorageNodeExecutor;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
//
import com.emc.mongoose.util.threading.ExecutorShutDownTask;
import com.emc.mongoose.util.threading.WorkerFactory;
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import javax.management.MBeanServer;
import java.io.Closeable;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public abstract class LoadExecutorBase<T extends DataItem>
extends Thread
implements LoadExecutor<T> {
	//
	private final Logger LOG = LogManager.getLogger();
	//
	protected final int threadsPerNode, retryCountMax, retryDelayMilliSec;
	protected final StorageNodeExecutor<T> nodes[];
	protected final ThreadPoolExecutor submitExecutor;
	protected final Closeable client;
	//
	protected final DataSource<T> dataSrc;
	protected volatile RunTimeConfig runTimeConfig = Main.RUN_TIME_CONFIG.get();
	//
	// METRICS section BEGIN
	protected final MetricRegistry metrics = new MetricRegistry();
	protected final Counter counterSubm, counterRej, counterReqSucc, counterReqFail;
	protected final Meter reqBytes;
	protected Histogram reqDur, respLatency;
	//
	protected final MBeanServer mBeanServer;
	protected final JmxReporter metricsReporter;
	// METRICS section END
	protected volatile Producer<T> producer = null;
	protected volatile Consumer<T> consumer;
	private volatile static int instanceN = 0;
	protected volatile long maxCount, tsStart;
	//
	private final Lock lock = new ReentrantLock();
	private final Condition condDone = lock.newCondition();
	private volatile boolean isClosed = false;
	//
	public static int getLastInstanceNum() {
		return instanceN;
	}
	//
	public static void setLastInstanceNum(final int lastInstanceN) {
		instanceN = lastInstanceN;
	}
	//
	@SuppressWarnings("unchecked")
	protected LoadExecutorBase(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final RequestConfig<T> reqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) throws ClassCastException {
		//
		this.runTimeConfig = runTimeConfig;
		//
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		mBeanServer = ServiceUtils.getMBeanServer(runTimeConfig.getRemoteExportPort());
		metricsReporter  = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		//
		final int
			nodeCount = addrs.length,
			loadNumber = instanceN ++;
		final String name = Integer.toString(loadNumber) + '-' +
			StringUtils.capitalize(reqConf.getAPI().toLowerCase()) + '-' +
			StringUtils.capitalize(reqConf.getLoadType().toString().toLowerCase()) +
			(maxCount>0? Long.toString(maxCount) : "") + '-' +
			Integer.toString(threadsPerNode) + 'x' + Integer.toString(nodeCount);
		setName(name);
		this.threadsPerNode = threadsPerNode;
		this.maxCount = maxCount>0? maxCount : Long.MAX_VALUE;
		// init metrics
		counterSubm = metrics.counter(MetricRegistry.name(name, METRIC_NAME_SUBM));
		counterRej = metrics.counter(MetricRegistry.name(name, METRIC_NAME_REJ));
		counterReqSucc = metrics.counter(MetricRegistry.name(name, METRIC_NAME_SUCC));
		counterReqFail = metrics.counter(MetricRegistry.name(name, METRIC_NAME_FAIL));
		reqBytes = metrics.meter(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_BW));
		reqDur = metrics.histogram(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_DUR));
		respLatency = metrics.histogram(MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_LAT));
		metricsReporter.start();
		// prepare the node executors array
		nodes = new StorageNodeExecutor[nodeCount];
		// create and configure the connection manager
		dataSrc = reqConf.getDataSource();
		setFileBasedProducer(listFile);
		//
		final int
			submitThreadCount = addrs.length * (int) Math.pow(threadsPerNode, 0.8),
			queueSize = submitThreadCount * runTimeConfig.getRunRequestQueueFactor();
		submitExecutor = new ThreadPoolExecutor(
			submitThreadCount, submitThreadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(queueSize),
			new WorkerFactory("submitDataItems")
		) {
			@Override
			protected final void terminated() {
				LOG.debug(Markers.MSG, "{}: submit executor terminated", getName());
				if(lock.tryLock()) {
					try {
						condDone.signalAll();
					} finally {
						lock.unlock();
					}
				} else {
					LOG.warn(Markers.ERR, "{}: failed to obtain the lock", getName());
				}
				super.terminated();
			}
		};
		submitExecutor.prestartAllCoreThreads();
		//
		client = initClient(addrs, reqConf);
		initNodeExecutors(addrs, reqConf.clone().setLoadNumber(loadNumber));
		// by default, may be overriden later externally
		setConsumer(new LogConsumer<T>());
	}
	//
	protected abstract void setFileBasedProducer(final String listFile);
	protected abstract Closeable initClient(final String addrs[], final RequestConfig<T> reqConf);
	protected abstract void initNodeExecutors(
		final String addrs[], final RequestConfig<T> reqConf
	) throws ClassCastException;
	//
	@Override
	public void start() {
		//
		if(producer == null) {
			LOG.debug(Markers.MSG, "{}: using an external data items producer", getName());
		} else {
			try {
				producer.start();
				LOG.debug(
					Markers.MSG, "Started object producer {}",
					producer.getClass().getSimpleName()
				);
			} catch(final IOException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to stop the producer");
			}
		}
		//
		LoadCloseHook.add(this);
		//
		tsStart = System.nanoTime();
		super.start();
		LOG.info(Markers.MSG, "Started \"{}\"", getName());
	}
	//
	@Override
	public final void run() {
		//
		final int metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
		try {
			if(metricsUpdatePeriodSec > 0) {
				while(isAlive()) {
					logMetrics(Markers.PERF_AVG);
					if(lock.tryLock()) {
						try {
							if(condDone.await(metricsUpdatePeriodSec, TimeUnit.SECONDS)) {
								LOG.debug(Markers.MSG, "Condition \"done\" reached");
								break;
							}
						} finally {
							lock.unlock();
						}
					} else {
						LOG.warn(Markers.ERR, "Failed to take the lock");
					}
				}
			} else if(lock.tryLock()) {
				try {
					condDone.await();
					LOG.debug(Markers.MSG, "Condition \"done\" reached");
				} finally {
					lock.unlock();
				}
			} else {
				LOG.error(Markers.ERR, "Failed to obtain the lock");
			}
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		} finally {
			LOG.debug(Markers.MSG, "Finish reached");
		}
		//
		LOG.trace(Markers.MSG, "Finish reached");
		//
		interrupt();
	}
	//
	private final static class InterruptProducerTask
	implements Runnable {
		//
		private final static Logger LOG = LogManager.getLogger();
		//
		Producer producer;
		//
		protected InterruptProducerTask(final Producer producer) {
			this.producer = producer;
		}
		//
		@Override
		public final void run() {
			if(producer != null) {
				try {
					producer.interrupt();
					LOG.debug(Markers.MSG, "Stopped object producer {}", producer.toString());
				} catch(final IOException e) {
					ExceptionHandler.trace(
						LOG, Level.WARN, e,
						String.format("Failed to stop the producer: %s", producer.toString())
					);
				}
			}
		}
	}
	//
	@Override
	public final synchronized void interrupt() {
		// set maxCount equal to current count
		maxCount = counterSubm.getCount() + counterRej.getCount();
		LOG.trace(Markers.MSG, "Interrupting, max count is set to {}", maxCount);
		//
		final ExecutorService interruptExecutor = Executors.newFixedThreadPool(nodes.length);
		//
		interruptExecutor.submit(new InterruptProducerTask(producer));
		interruptExecutor.submit(new ExecutorShutDownTask(submitExecutor, runTimeConfig));
		// interrupt node executors
		for(final StorageNodeExecutor<T> nextNode: nodes) {
			interruptExecutor.submit(
				new Runnable() {
					@Override
					public final void run() {
						ThreadPoolExecutor.class.cast(nextNode).shutdown();
					}
				}
			);
		}
		//
		interruptExecutor.shutdown();
		try {
			interruptExecutor.awaitTermination(
				Main.RUN_TIME_CONFIG.get().getRunReqTimeOutMilliSec(), TimeUnit.MILLISECONDS
			);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.ERR, "Interrupted externally");
		}
		// interrupt the monitoring thread
		if(!isInterrupted()) {
			super.interrupt();
		}
	}
	//
	@Override
	public synchronized void close()
	throws IOException {
		//
		if(!isClosed) { // this is just not-closed-yet flag
			LOG.debug(Markers.MSG, "{}: do invoking close", getName());
			if(!isInterrupted()) {
				interrupt();
			}
			// poison the consumer
			try {
				consumer.submit(null);
			} catch(final Exception e) {
				ExceptionHandler.trace(LOG, Level.DEBUG, e, "Failed to feed the poison");
			}
			// provide summary metrics
			logMetrics(Markers.PERF_SUM);
			// close node executors
			final ArrayList<Thread> nodeClosers = new ArrayList<>(nodes.length);
			Thread nextShutDownThread;
			for(final StorageNodeExecutor<T> nodeExecutor : nodes) {
				nextShutDownThread = new Thread("closeNodeExecutor-" + nodeExecutor.toString()) {
					@Override
					public final void run() {
						try {
							nodeExecutor.close();
						} catch(final IOException e) {
							ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to close node executor");
						}
					}
				};
				nextShutDownThread.start();
				nodeClosers.add(nextShutDownThread);
			}
			//
			for(final Thread nextClosingThread : nodeClosers) {
				try {
					nextClosingThread.join(runTimeConfig.getRunReqTimeOutMilliSec());
				} catch(final InterruptedException e) {
					ExceptionHandler.trace(LOG, Level.WARN, e, "Interrupted closing node executor");
				}
			}
			//
			metricsReporter.close();
			//
			if(client!=null) {
				try {
					client.close();
					LOG.debug(Markers.MSG, "Storage client closed");
				} catch(final IOException e) {
					ExceptionHandler.trace(LOG, Level.WARN, e, "Storage client closing failed");
				}
			}
			//
			LoadCloseHook.del(this);
			isClosed = true;
			LOG.debug(Markers.MSG, "Closed {}", getName());
		} else {
			LOG.debug(Markers.ERR, "Closed already");
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void submit(final T dataItem)
	throws RemoteException, InterruptedException {
		if(maxCount > counterRej.getCount() + counterReqFail.getCount() + counterReqSucc.getCount()) {
			if(dataItem == null) {
				LOG.debug(Markers.MSG, "{}: poison submitted", getName());
				// determine the max count now
				maxCount = counterSubm.getCount() + counterRej.getCount();
				// pass the poison to all node executors
				for(final StorageNodeExecutor<T> nextNode: nodes) {
					if(!nextNode.isShutdown()) {
						nextNode.submit(null);
					}
				}
				//
				submitExecutor.shutdown();
			} else {
				final StorageNodeExecutor<T> nodeExecutor = nodes[
					(int) submitExecutor.getTaskCount() % nodes.length
				];
				final SubmitDataItemTask<T, StorageNodeExecutor<T>>
					submitTask = new SubmitDataItemTask<>(dataItem, nodeExecutor);
				boolean flagSubmSucc = false;
				int rejectCount = 0;
				while(
					!flagSubmSucc && rejectCount < retryCountMax &&
						!submitExecutor.isShutdown() && !nodeExecutor.isShutdown()
					) {
					try {
						submitExecutor.submit(submitTask);
						flagSubmSucc = true;
					} catch(final RejectedExecutionException e) {
						rejectCount ++;
						try {
							Thread.sleep(rejectCount * retryDelayMilliSec);
						} catch(final InterruptedException ee) {
							break;
						}
					}
				}
			}
		} else {
			submitExecutor.shutdown();
			throw new InterruptedException(
				String.format(
					"%s: max data item count (%d) have been submitted, shutdown the submit executor",
					getName(), maxCount
				)
			);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static String FMT_EFF_SUM = "Load execution efficiency: %.1f[%%]";
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
		final Snapshot
			reqDurSnapshot = reqDur.getSnapshot(),
			respLatencySnapshot = respLatency.getSnapshot();
		//
		int notCompletedTaskCount = 0;
		for(final StorageNodeExecutor<T> nodeExecutor: nodes) {
			notCompletedTaskCount += nodeExecutor.getQueue().size() + nodeExecutor.getActiveCount();
		}
		notCompletedTaskCount += submitExecutor.getQueue().size() + submitExecutor.getActiveCount();
		//
		final String message = Markers.PERF_SUM.equals(logMarker) ?
			String.format(
				Locale.ROOT, MSG_FMT_SUM_METRICS,
				//
				getName(),
				countReqSucc, counterReqFail.getCount(),
				//
				(float) reqDurSnapshot.getMean() / BILLION,
				(float) reqDurSnapshot.getMin() / BILLION,
				(float) reqDurSnapshot.getMedian() / BILLION,
				(float) reqDurSnapshot.getMax() / BILLION,
				//
				(float) respLatencySnapshot.getMean() / BILLION,
				(float) respLatencySnapshot.getMin() / BILLION,
				(float) respLatencySnapshot.getMedian() / BILLION,
				(float) respLatencySnapshot.getMax() / BILLION,
				//
				avgSize == 0 ? 0 : meanBW / avgSize,
				avgSize == 0 ? 0 : oneMinBW / avgSize,
				avgSize == 0 ? 0 : fiveMinBW / avgSize,
				avgSize == 0 ? 0 : fifteenMinBW / avgSize,
				//
				meanBW / MIB,
				oneMinBW / MIB,
				fiveMinBW / MIB,
				fifteenMinBW / MIB
			) :
			String.format(
				Locale.ROOT, MSG_FMT_METRICS,
				//
				countReqSucc, notCompletedTaskCount, counterReqFail.getCount(),
				//
				(float) reqDurSnapshot.getMean() / BILLION,
				(float) reqDurSnapshot.getMin() / BILLION,
				(float) reqDurSnapshot.getMedian() / BILLION,
				(float) reqDurSnapshot.getMax() / BILLION,
				//
				(float) respLatencySnapshot.getMean() / BILLION,
				(float) respLatencySnapshot.getMin() / BILLION,
				(float) respLatencySnapshot.getMedian() / BILLION,
				(float) respLatencySnapshot.getMax() / BILLION,
				//
				avgSize == 0 ? 0 : meanBW / avgSize,
				avgSize == 0 ? 0 : oneMinBW / avgSize,
				avgSize == 0 ? 0 : fiveMinBW / avgSize,
				avgSize == 0 ? 0 : fifteenMinBW / avgSize,
				//
				meanBW / MIB,
				oneMinBW / MIB,
				fiveMinBW / MIB,
				fifteenMinBW / MIB
			);
		LOG.info(logMarker, message);
		//
		if(Markers.PERF_SUM.equals(logMarker)) {
			final double totalReqNanoSeconds = reqDurSnapshot.getMean() * countReqSucc;
			LOG.debug(
				Markers.PERF_SUM,
				String.format(
					Locale.ROOT, FMT_EFF_SUM,
					100 * totalReqNanoSeconds / ((System.nanoTime() - tsStart) * getThreadCount())
				)
			);
		}
		//
		if(LOG.isTraceEnabled(Markers.PERF_AVG)) {
			for(final StorageNodeExecutor<T> node: nodes) {
				node.logMetrics(Level.TRACE, Markers.PERF_AVG);
			}
		}
		//
	}
	//
	@Override
	public final long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public final void setMaxCount(final long maxCount) {
		this.maxCount = maxCount;
	}
	//
	public final int getThreadCount() {
		return threadsPerNode * nodes.length;
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
		for(final StorageNodeExecutor<T> node: nodes) {
			node.setConsumer(consumer);
		}
		LOG.debug(
			Markers.MSG, "Appended the consumer \"{}\" for producer \"{}\"", consumer, getName()
		);
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
	//
	@Override
	public final Producer<T> getProducer() {
		return producer;
	}
	//
	public final DataSource<T> getDataSource() {
		return dataSrc;
	}
}
