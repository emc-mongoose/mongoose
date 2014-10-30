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
import com.emc.mongoose.util.persist.LoadEntity;
import com.emc.mongoose.util.persist.LoadTypeEntity;
import com.emc.mongoose.util.remote.ServiceUtils;
//
import com.emc.mongoose.util.threading.GentleExecutorShutDown;
import com.emc.mongoose.util.threading.WorkerFactory;
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import javax.management.MBeanServer;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
	//
	protected final DataSource<T> dataSrc;
	protected volatile RunTimeConfig runTimeConfig = Main.RUN_TIME_CONFIG;
	//
	// METRICS section BEGIN
	protected final MetricRegistry metrics = new MetricRegistry();
	protected final Counter counterSubm, counterRej, counterReqSucc, counterReqFail;
	protected final Meter reqBytes;
	protected Histogram reqDur;
	//
	protected final MBeanServer mBeanServer;
	protected final JmxReporter metricsReporter;
	// METRICS section END
	protected volatile Producer<T> producer = null;
	protected volatile Consumer<T> consumer;
	private volatile static int instanceN = 0;
	protected volatile long maxCount, tsStart;
	protected LoadTypeEntity type = new LoadTypeEntity();
	protected LoadEntity load ;
	//
	private final static String KEY_NODE_ADDR = "node.addr",
								KEY_LOAD_NUM = "load.number",
								KEY_LOAD_TYPE = "load.type",
								KEY_API = "api";
	private static  final Map<String,String> context = new HashMap<String,String>();
	public static Map<String,String> getContext() { return context; }
	public static void addContextElement(final String key, final String value) { context.put(key,value); }
	//
	public static int getLastInstanceNum() {
		return instanceN;
	}
	//
	public static void setLastInstanceNum(final int lastInstanceN) {
		instanceN = lastInstanceN;
	}
	//
	private static String api;
	public static String getApi(){ return api; };
	private static String loadType;
	public static String getLoadType(){ return loadType; };
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
		api = reqConf.getAPI();
		loadType = reqConf.getLoadType().toString();
		//
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		mBeanServer = ServiceUtils.getMBeanServer(runTimeConfig.getRemoteMonitorPort());
		metricsReporter  = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		//
		final int nodeCount = addrs.length;
		final String name = Integer.toString(instanceN++) + '-' +
			StringUtils.capitalize(api.toLowerCase()) + '-' +
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
		metricsReporter.start();
		// prepare the node executors array
		nodes = new StorageNodeExecutor[nodeCount];
		// create and configure the connection manager
		dataSrc = reqConf.getDataSource();
		setFileBasedProducer(listFile);
		final int
			submitThreadCount = threadsPerNode * addrs.length,
			queueSize = submitThreadCount * runTimeConfig.getRunRequestQueueFactor();
		//Put value in thread context map
		context.put(KEY_NODE_ADDR,reqConf.getAddr());
		context.put(KEY_LOAD_NUM, String.valueOf(getLastInstanceNum()));
		context.put(KEY_API,reqConf.getAPI());
		context.put(KEY_LOAD_TYPE,reqConf.getLoadType().toString());
		//
		submitExecutor = new ThreadPoolExecutor(
			submitThreadCount, submitThreadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(queueSize),
			new WorkerFactory("submitDataItems", context)
		);
		initClient(addrs, reqConf);
		initNodeExecutors(addrs, reqConf);
		// by default, may be overriden later externally
		setConsumer(new LogConsumer<T>());
	}
	//
	protected abstract void setFileBasedProducer(final String listFile);
	protected abstract void initClient(final String addrs[], final RequestConfig<T> reqConf);
	protected abstract void initNodeExecutors(
		final String addrs[], final RequestConfig<T> reqConf
	) throws ClassCastException;
	//
	@Override
	public void start() {
		//
		if(producer == null) {
			LOG.info(Markers.MSG, "Waiting for incoming objects to process from external producer");
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
		ShutDownHook.add(this);
		//
		tsStart = System.nanoTime();
		super.start();
		LOG.debug(Markers.MSG, "Started {}", getName());
	}
	//
	@Override
	public final void run() {
		//
		try {
			final int metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
			do {
				logMetrics(Markers.PERF_AVG);
				TimeUnit.SECONDS.sleep(metricsUpdatePeriodSec); // sleep
			} while(maxCount > counterReqSucc.getCount() + counterReqFail.getCount() + counterRej.getCount());
			LOG.trace(Markers.MSG, "Max count reached");
		} catch(final InterruptedException e) {
			LOG.trace(Markers.MSG, "Externally interrupted \"{}\"", getName());
		}
		//
		interrupt();
		//
	}
	//
	@Override
	public final synchronized void interrupt() {
		// set maxCount equal to current count
		maxCount = counterSubm.getCount() + counterRej.getCount();
		LOG.trace(Markers.MSG, "Interrupting, max count is set to {}", maxCount);
		//
		final Thread interrupters[] = new Thread[nodes.length < 2 ? 2 : nodes.length];
		// interrupt a producer
		interrupters[0] = new Thread("interrupt-producer-" + getName()) {
			@Override
			public final void run() {
				if(producer!=null) {
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
		};
		// interrupt the submit execution
		interrupters[1] = new Thread(
			new GentleExecutorShutDown(submitExecutor, runTimeConfig),
			String.format("interrupt-submit-%s", getName())
		);
		interrupters[1].start();
		//
		try {
			interrupters[0].join();
		} catch(final InterruptedException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted while interrupting producer");
		}
		try {
			interrupters[1].join();
		} catch(final InterruptedException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted while interrupting submitter");
		}
		// interrupt node executors in parallel threads
		for(int i = 0; i < nodes.length; i ++) {
			final StorageNodeExecutor<T> nextNode = nodes[i];
			interrupters[i] = new Thread("interrupt-" + getName() + "-" + nextNode.getAddr()) {
				@Override
				public final void run() {
					nextNode.interrupt();
				}
			};
			interrupters[i].start();

		}
		// wait for node executors to become interrupted
		final int reqTimeOutMilliSec = runTimeConfig.getRunReqTimeOutMilliSec();
		for(final Thread interrupter : interrupters) {
			try {
				interrupter.join(reqTimeOutMilliSec);
				LOG.debug(Markers.MSG, "Finished: \"{}\"", interrupter.getName());
			} catch(final InterruptedException e) {
				ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted while interrupting the node executor");
			}
		}
		// interrupt the monitoring thread
		super.interrupt();
		LOG.debug(Markers.MSG, "Interrupted \"{}\"", getName());
	}
	//
	@Override
	public synchronized void close()
		throws IOException {
		//
		if(!isInterrupted()) {
			interrupt();
		}
		consumer.submit(null); // poison the consumer
		for(final StorageNodeExecutor<T> nodeExecutor: nodes) {
			try {
				nodeExecutor.close();
				LOG.debug(Markers.MSG, "Closed the node executor {}", nodeExecutor);
			} catch(final IOException e) {
				ExceptionHandler.trace(
					LOG, Level.WARN, e,
					String.format("Failed to stop the node executor: %s", nodeExecutor.getName())
				);
			}
		}
		// provide summary metrics
		synchronized(LOG) {
			LOG.info(Markers.PERF_SUM, "Summary metrics below for {}", getName());
			logMetrics(Markers.PERF_SUM);
		}
		metricsReporter.close();
		//
		LOG.debug(Markers.MSG, "Closed {}", getName());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void submit(final T dataItem)
	throws RemoteException {
		if(dataItem == null || isInterrupted()) { // handle the poison
			maxCount = counterSubm.getCount() + counterRej.getCount();
			LOG.debug(Markers.MSG, "Poisoned on #{}", maxCount);
			for(final StorageNodeExecutor<T> nextNode: nodes) {
				if(!nextNode.isShutdown()) {
					nextNode.submit(null); // poison
				}
			}
		} else {
			final SubmitDataItemTask<T, StorageNodeExecutor<T>>
				submitTask = new SubmitDataItemTask<>(
				dataItem, nodes[(int) submitExecutor.getTaskCount() % nodes.length]
			);
			boolean flagSubmSucc = false;
			int rejectCount = 0;
			do {
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
			} while(!flagSubmSucc && rejectCount < retryCountMax && !submitExecutor.isShutdown());
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
		final Snapshot reqDurSnapshot = reqDur.getSnapshot();
		//
		int notCompletedTaskCount = 0;
		for(final StorageNodeExecutor<T> nodeExecutor: nodes) {
			notCompletedTaskCount += nodeExecutor.getQueue().size() + nodeExecutor.getActiveCount();
		}
		notCompletedTaskCount += submitExecutor.getQueue().size() + submitExecutor.getActiveCount();
		//
		final String message = MSG_FMT_METRICS.format(
				new Object[]{
						countReqSucc, notCompletedTaskCount, counterReqFail.getCount(),
						//
						(float) reqDurSnapshot.getMin() / BILLION,
						(float) reqDurSnapshot.getMedian() / BILLION,
						(float) reqDurSnapshot.getMean() / BILLION,
						(float) reqDurSnapshot.getMax() / BILLION,
						//
						avgSize == 0 ? 0 : meanBW / avgSize,
						avgSize == 0 ? 0 : oneMinBW / avgSize,
						avgSize == 0 ? 0 : fiveMinBW / avgSize,
						avgSize == 0 ? 0 : fifteenMinBW / avgSize,
						//
						meanBW / MIB, oneMinBW / MIB, fiveMinBW / MIB, fifteenMinBW / MIB
				}
		);
		LOG.info(logMarker, message);
		//PersistDAO.setMessage(new Date(), this.getClass().getSimpleName(), "INFO", message);
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
	//
}
