package com.emc.mongoose.base.load.client.impl;
//
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.persist.LogConsumer;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.base.load.client.DataItemBufferClient;
import com.emc.mongoose.base.load.client.impl.gauges.AvgDouble;
import com.emc.mongoose.base.load.client.impl.gauges.MaxLong;
import com.emc.mongoose.base.load.client.impl.gauges.MinLong;
import com.emc.mongoose.base.load.client.impl.gauges.SumDouble;
import com.emc.mongoose.base.load.client.impl.gauges.SumLong;
import com.emc.mongoose.base.load.client.impl.gauges.ThroughPut;
import com.emc.mongoose.base.load.client.impl.tasks.CountLimitWaitTask;
import com.emc.mongoose.base.load.client.impl.tasks.FrameFetchTask;
import com.emc.mongoose.base.load.client.impl.tasks.GaugeValueTask;
import com.emc.mongoose.base.load.client.impl.tasks.InterruptSvcTask;
import com.emc.mongoose.base.load.client.impl.tasks.RemoteJoinTask;
import com.emc.mongoose.base.load.client.impl.tasks.RemoteSubmitTask;
import com.emc.mongoose.base.load.impl.tasks.LoadCloseHook;
import com.emc.mongoose.base.load.client.LoadClient;
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
import com.emc.mongoose.util.threading.WorkerFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
//
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 20.10.14.
 */
public class BasicLoadClient<T extends DataItem>
extends ThreadPoolExecutor
implements LoadClient<T> {
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final Map<String, LoadSvc<T>> remoteLoadMap;
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final Map<String, JMXConnector> remoteJMXConnMap;
	private final Map<String, MBeanServerConnection> mBeanSrvConnMap;
	private final MetricRegistry metrics = new MetricRegistry();
	protected final JmxReporter metricsReporter;
	//
	@SuppressWarnings("FieldCanBeLocal")
	private final Gauge<Long>
		metricSuccCount, metricByteCount;
	@SuppressWarnings("FieldCanBeLocal")
	private final Gauge<Double>
		metricBWMean, metricBW1Min, metricBW5Min, metricBW15Min;
	@SuppressWarnings("FieldCanBeLocal")
	private final GaugeValueTask<Long>
		taskGetCountSubm, taskGetCountRej, taskGetCountSucc, taskGetCountFail,
		/*taskGetDurMin, taskGetDurMax,*/ taskGetLatencyMin, taskGetLatencyMax,
		taskGetCountBytes, taskGetCountNanoSec;
	private final GaugeValueTask<Double>
		taskGetTPMean, taskGetTP1Min, taskGetTP5Min, taskGetTP15Min,
		taskGetBWMean, taskGetBW1Min, taskGetBW5Min, taskGetBW15Min,
		/*taskGetDurMed, taskGetDurAvg,*/ taskGetLatencyMed, taskGetLatencyAvg;
	//
	private final ExecutorService mgmtConnExecutor;
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final long maxCount;
	private final String name, loadSvcAddrs[];
	private final Thread shutDownOnCountLimit;
	//
	private final LogConsumer<T> metaInfoLog;
	private final RunTimeConfig runTimeConfig;
	private final RequestConfig<T> reqConfig;
	private final int retryCountMax, retryDelayMilliSec;
	//
	public BasicLoadClient(
		final RunTimeConfig runTimeConfig,
		final Map<String, LoadSvc<T>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap,
		final RequestConfig<T> reqConfig,
		final long maxCount, final int threadCountPerServer
	) {
		super(
			1, 1, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(runTimeConfig.getRunRequestQueueSize())
		);
		final int totalThreadCount = threadCountPerServer * remoteLoadMap.size();
		setCorePoolSize(totalThreadCount);
		setMaximumPoolSize(totalThreadCount);
		//
		String t = null;
		try {
			final Object remoteLoads[] = remoteLoadMap.values().toArray();
			t = LoadSvc.class.cast(remoteLoads[0]).getName() + 'x' + remoteLoads.length;
		} catch(final NoSuchElementException | NullPointerException e) {
			LOG.error(Markers.ERR, "No remote load instances", e);
		} catch(final IOException e) {
			LOG.error(Markers.ERR, "Looks like connectivity failure", e);
		}
		name = t;
		//
		setThreadFactory(new WorkerFactory(name + "-clientSubmitWorker"));
		//
		this.runTimeConfig = runTimeConfig;
		this.reqConfig = reqConfig;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		//
		final MBeanServer mBeanServer = ServiceUtils.getMBeanServer(
			runTimeConfig.getRemoteExportPort()
		);
		metricsReporter = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		////////////////////////////////////////////////////////////////////////////////////////////
		this.remoteLoadMap = remoteLoadMap;
		this.loadSvcAddrs = (String[]) remoteLoadMap.keySet().toArray();
		this.remoteJMXConnMap = remoteJMXConnMap;
		////////////////////////////////////////////////////////////////////////////////////////////
		mBeanSrvConnMap = new HashMap<>();
		for(final String addr: loadSvcAddrs) {
			try {
				mBeanSrvConnMap.put(addr, remoteJMXConnMap.get(addr).getMBeanServerConnection());
			} catch(final IOException e) {
				ExceptionHandler.trace(
					LOG, Level.ERROR, e,
					String.format("Failed to obtain MBean server connection for %s", addr)
				);
			}
		}
		//
		mgmtConnExecutor = Executors.newFixedThreadPool(
			totalThreadCount, new WorkerFactory(name + "-mgmtConnWorker")
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		metricSuccCount = registerJmxGaugeSum(
			DEFAULT_DOMAIN, METRIC_NAME_SUCC, ATTR_COUNT
		);
		metricByteCount = registerJmxGaugeSum(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_BW, ATTR_COUNT
		);
		metricBWMean = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_BW, ATTR_RATE_MEAN
		);
		metricBW1Min = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_BW, ATTR_RATE_1MIN
		);
		metricBW5Min = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ+"."+METRIC_NAME_BW, ATTR_RATE_5MIN
		);
		metricBW15Min = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_BW, ATTR_RATE_15MIN
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		taskGetCountSubm = new GaugeValueTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_SUBM, ATTR_COUNT)
		);
		taskGetCountRej = new GaugeValueTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_REJ, ATTR_COUNT)
		);
		taskGetCountSucc = new GaugeValueTask<>(metricSuccCount);
		taskGetCountFail = new GaugeValueTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_FAIL, ATTR_COUNT)
		);
		taskGetCountNanoSec = new GaugeValueTask<>(
			registerJmxGaugeSum(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_COUNT
			)
		);
		taskGetCountBytes = new GaugeValueTask<>(metricByteCount);
		/*taskGetDurMin = new GaugeValueTask<>(
			registerJmxGaugeMinLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MIN
			)
		);
		taskGetDurMax = new GaugeValueTask<>(
			registerJmxGaugeMaxLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MAX
			)
		);*/
		taskGetLatencyMin = new GaugeValueTask<>(
			registerJmxGaugeMinLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MIN
			)
		);
		taskGetLatencyMax = new GaugeValueTask<>(
			registerJmxGaugeMaxLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MAX
			)
		);
		taskGetBWMean = new GaugeValueTask<>(metricBWMean);
		taskGetBW1Min = new GaugeValueTask<>(metricBW1Min);
		taskGetBW5Min = new GaugeValueTask<>(metricBW5Min);
		taskGetBW15Min = new GaugeValueTask<>(metricBW15Min);
		taskGetTPMean = new GaugeValueTask<>(
			metrics.register(
				MetricRegistry.name(name, METRIC_NAME_TP + "." + ATTR_RATE_MEAN),
				new ThroughPut(mgmtConnExecutor, taskGetBWMean, taskGetCountSucc, taskGetCountBytes)
			)
		);
		taskGetTP1Min = new GaugeValueTask<>(
			metrics.register(
				MetricRegistry.name(name, METRIC_NAME_TP + "." + ATTR_RATE_1MIN),
				new ThroughPut(mgmtConnExecutor, taskGetBW1Min, taskGetCountSucc, taskGetCountBytes)
			)
		);
		taskGetTP5Min = new GaugeValueTask<>(
			metrics.register(
				MetricRegistry.name(name, METRIC_NAME_TP + "." + ATTR_RATE_5MIN),
				new ThroughPut(mgmtConnExecutor, taskGetBW5Min, taskGetCountSucc, taskGetCountBytes)
			)
		);
		taskGetTP15Min = new GaugeValueTask<>(
			metrics.register(
				MetricRegistry.name(name, METRIC_NAME_TP + "." + ATTR_RATE_15MIN),
				new ThroughPut(mgmtConnExecutor, taskGetBW15Min, taskGetCountSucc, taskGetCountBytes)
			)
		);
		/*taskGetDurMed = new GaugeValueTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MED
			)
		);
		taskGetDurAvg = new GaugeValueTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_AVG
			)
		);*/
		taskGetLatencyMed = new GaugeValueTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MED
			)
		);
		taskGetLatencyAvg = new GaugeValueTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_AVG
			)
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		metaInfoLog = new LogConsumer<>(maxCount, threadCountPerServer);
		shutDownOnCountLimit = new Thread(
			new CountLimitWaitTask(
				this, mgmtConnExecutor, maxCount,
				new GaugeValueTask[] {taskGetCountSucc, taskGetCountFail, taskGetCountRej}
			),
			"shutDownOnCountLimit"
		);
		shutDownOnCountLimit.start();
		prestartAllCoreThreads();
		metricsReporter.start();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private Gauge<Long> registerJmxGaugeSum(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName + "." + attrName),
			new SumLong(domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	//
	private Gauge<Long> registerJmxGaugeMinLong(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new MinLong(domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	//
	private Gauge<Long> registerJmxGaugeMaxLong(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new MaxLong(domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	//
	private Gauge<Double> registerJmxGaugeSumDouble(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new SumDouble(domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	//
	private Gauge<Double> registerJmxGaugeAvgDouble(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new AvgDouble(domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
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
	private final Thread aggregateThread = new Thread(getName() + "-dumpMetrics") {
		@Override
		public final void run() {
			final int metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
			if(metricsUpdatePeriodSec > 0) {
				while(isAlive()) {
					logMetaInfoFrames();
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
	private void logMetaInfoFrames() {
		final ArrayList<Future<List<T>>> nextMetaInfoFrameFutures = new ArrayList<>(
			remoteLoadMap.size()
		);
		//
		LoadSvc<T> nextLoadSvc;
		for(final String loadSvcAddr : loadSvcAddrs) {
			nextLoadSvc = remoteLoadMap.get(loadSvcAddr);
			try {
				nextMetaInfoFrameFutures.add(
					mgmtConnExecutor.submit(new FrameFetchTask<List<T>>(nextLoadSvc))
				);
			} catch(final RejectedExecutionException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Fetching metainfo frame task rejected");
			}
		}
		//
		List<T> nextMetaInfoFrame = null;
		for(final Future<List<T>> nextMetaInfoFrameFuture : nextMetaInfoFrameFutures) {
			//
			try {
				nextMetaInfoFrame = nextMetaInfoFrameFuture.get();
			} catch(final InterruptedException e) {
				try {
					nextMetaInfoFrame = nextMetaInfoFrameFuture.get();
				} catch(final InterruptedException|ExecutionException ee) {
					ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to fetch the metainfo frame");
				}
			} catch(final Exception e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to fetch the metainfo frame");
			}
			//
			if(nextMetaInfoFrame != null && nextMetaInfoFrame.size() > 0) {
				for(final T nextMetaInfoRec: nextMetaInfoFrame) {
					metaInfoLog.submit(nextMetaInfoRec);
				}
			}
			//
		}
	}
	//
	protected Future<Long>
		countSubm, countRej, countReqSucc, countReqFail,
		/*countNanoSec, countBytes, */minDur, maxDur,
		minLatency, maxLatency;
	protected Future<Double>
		meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
		meanBW, oneMinBW, fiveMinBW, fifteenMinBW,
		/*medDur, avgDur,*/ medLatency, avgLatency;
	//
	private void logMetrics(final Marker logMarker) {
		//
		if(!isShutdown()) {
			try {
				countSubm = mgmtConnExecutor.submit(taskGetCountSubm);
				countRej = mgmtConnExecutor.submit(taskGetCountRej);
				countReqSucc = mgmtConnExecutor.submit(taskGetCountSucc);
				countReqFail = mgmtConnExecutor.submit(taskGetCountFail);
				//countNanoSec = mgmtConnExecutor.submit(taskGetCountNanoSec);
				//countBytes = mgmtConnExecutor.submit(taskGetCountBytes);
				//minDur = mgmtConnExecutor.submit(taskGetDurMin);
				//maxDur = mgmtConnExecutor.submit(taskGetDurMax);
				minLatency = mgmtConnExecutor.submit(taskGetLatencyMin);
				maxLatency = mgmtConnExecutor.submit(taskGetLatencyMax);
				meanTP = mgmtConnExecutor.submit(taskGetTPMean);
				oneMinTP = mgmtConnExecutor.submit(taskGetTP1Min);
				fiveMinTP = mgmtConnExecutor.submit(taskGetTP5Min);
				fifteenMinTP = mgmtConnExecutor.submit(taskGetTP15Min);
				meanBW = mgmtConnExecutor.submit(taskGetBWMean);
				oneMinBW = mgmtConnExecutor.submit(taskGetBW1Min);
				fiveMinBW = mgmtConnExecutor.submit(taskGetBW5Min);
				fifteenMinBW = mgmtConnExecutor.submit(taskGetBW15Min);
				//medDur = mgmtConnExecutor.submit(taskGetDurMed);
				//avgDur = mgmtConnExecutor.submit(taskGetDurAvg);
				medLatency = mgmtConnExecutor.submit(taskGetLatencyMed);
				avgLatency = mgmtConnExecutor.submit(taskGetLatencyAvg);
			} catch(final RejectedExecutionException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Log remote metrics failed due to reject");
			}
		}
		//
		try {
			LOG.info(
				logMarker,
				Markers.PERF_SUM.equals(logMarker) ?
				String.format(
					Main.LOCALE_DEFAULT, MSG_FMT_SUM_METRICS,
					//
					name,
					countReqSucc.get(), countReqFail.get(),
					//
					avgLatency.get() / NANOSEC_SCALEDOWN, (double) minLatency.get() / NANOSEC_SCALEDOWN,
					medLatency.get() / NANOSEC_SCALEDOWN, (double) maxLatency.get() / NANOSEC_SCALEDOWN,
					//
					meanTP.get(), oneMinTP.get(), fiveMinTP.get(), fifteenMinTP.get(),
					//
					meanBW.get() / MIB, oneMinBW.get() / MIB, fiveMinBW.get() / MIB,
					fifteenMinBW.get() / MIB
				) :
				String.format(
					Main.LOCALE_DEFAULT, MSG_FMT_METRICS,
					//
					countReqSucc.get(),
					getQueue().size() + getActiveCount(),
					countReqFail.get(),
					//
					avgLatency.get() / NANOSEC_SCALEDOWN, (double) minLatency.get() / NANOSEC_SCALEDOWN,
					medLatency.get() / NANOSEC_SCALEDOWN, (double) maxLatency.get() / NANOSEC_SCALEDOWN,
					//
					meanTP.get(), oneMinTP.get(), fiveMinTP.get(), fifteenMinTP.get(),
					//
					meanBW.get() / MIB, oneMinBW.get() / MIB, fiveMinBW.get() / MIB,
					fifteenMinBW.get() / MIB
				)
			);
		} catch(final ExecutionException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to fetch the metrics");
		} catch(final InterruptedException | NullPointerException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Unexpected failure");
		}
		//
	}
	//
	@Override
	public final void start() {
		if(aggregateThread.isAlive()) {
			LOG.warn(Markers.ERR, "{}: already started");
		} else {
			LoadSvc nextLoadSvc;
			for(final String addr : loadSvcAddrs) {
				nextLoadSvc = remoteLoadMap.get(addr);
				try {
					nextLoadSvc.start();
					LOG.debug(
						Markers.MSG, "{} started bound to remote service @{}",
						nextLoadSvc.getName(), addr
					);
				} catch(final IOException e) {
					LOG.error(Markers.ERR, "Failed to start remote load @" + addr, e);
				}
			}
			//
			LoadCloseHook.add(this);
			//
			aggregateThread.start();
			LOG.info(Markers.MSG, "{}: started", name);
		}
	}
	//
	@Override
	public final void interrupt() {
		final int reqTimeOutMilliSec = runTimeConfig.getRunReqTimeOutMilliSec();
		//
		if(!isShutdown()) {
			shutdown();
		}
		//
		if(aggregateThread.isAlive()) {
			LOG.debug(Markers.MSG, "{}: interrupting...", name);
			final ExecutorService interruptExecutor = Executors.newFixedThreadPool(
				remoteLoadMap.size()
			);
			for(final String addr : loadSvcAddrs) {
				interruptExecutor.submit(new InterruptSvcTask(remoteLoadMap.get(addr), addr));
			}
			interruptExecutor.shutdown();
			try {
				interruptExecutor.awaitTermination(reqTimeOutMilliSec, TimeUnit.MILLISECONDS);
			} catch(final InterruptedException e) {
				ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupting was interrupted");
			}
			aggregateThread.interrupt();
			LOG.debug(Markers.MSG, "{}: interrupted", name);
		}
	}
	//
	private volatile LoadClient<T> consumer = null;
	//
	@Override
	public final Consumer<T> getConsumer() {
		return consumer;
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer)
	throws RemoteException {
		if(LoadClient.class.isInstance(consumer)) {
			// consumer is client which has the map of consumers
			try {
				this.consumer = (LoadClient<T>) consumer;
				final Map<String, LoadSvc<T>> consumeMap = this.consumer.getRemoteLoadMap();
				LOG.debug(Markers.MSG, "Consumer is LoadClient instance");
				for(final String addr : consumeMap.keySet()) {
					remoteLoadMap.get(addr).setConsumer(consumeMap.get(addr));
				}
			} catch(final ClassCastException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Data item class mismatch");
			}
		} else if(LoadSvc.class.isInstance(consumer)) {
			// single consumer for all these producers
			try {
				final LoadSvc<T> loadSvc = (LoadSvc<T>) consumer;
				LOG.debug(Markers.MSG, "Consumer is load service instance");
				for(final String addr : loadSvcAddrs) {
					remoteLoadMap.get(addr).setConsumer(loadSvc);
				}
			} catch(final ClassCastException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Data item class mismatch");
			}
		} else if(DataItemBufferClient.class.isInstance(consumer)) {
			try {
				final DataItemBufferClient<T> mediator = (DataItemBufferClient<T>) consumer;
				LOG.debug(Markers.MSG, "Consumer is remote mediator buffer");
				for(final String addr : loadSvcAddrs) {
					remoteLoadMap.get(addr).setConsumer(mediator.get(addr));
				}
			} catch(final ClassCastException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Data item class mismatch");
			}
		} else {
			LOG.error(
				Markers.ERR, "Unexpected consumer type: {}",
				consumer == null ? null : consumer.getClass()
			);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void submit(final T dataItem)
	throws InterruptedException {
		if(maxCount > getTaskCount()) {
			if(dataItem == null) {
				LOG.debug(Markers.MSG, "{}: poison submitted, performing the shutdown", name);
				// poison the remote load service instances
				for(final String addr : loadSvcAddrs) {
					try {
						remoteLoadMap.get(addr).submit((T) null); // feed the poison
					} catch(final Exception e) {
						ExceptionHandler.trace(
							LOG, Level.WARN, e,
							String.format("Failed to submit the poison to @%s", addr)
						);
					}
				}
				//
				shutdown();
			} else {
				final String addr = loadSvcAddrs[(int) getTaskCount() % loadSvcAddrs.length];
				final RemoteSubmitTask<T> remoteSubmitTask = RemoteSubmitTask
					.getInstanceFor(remoteLoadMap.get(addr), dataItem);
				int rejectCount = 0;
				do {
					try {
						submit(remoteSubmitTask);
						break;
					} catch(final RejectedExecutionException e) {
						rejectCount ++;
						try {
							Thread.sleep(rejectCount * retryDelayMilliSec);
						} catch(final InterruptedException ee) {
							break;
						}
					}
				} while(rejectCount < retryCountMax && !isShutdown());
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
	public final void shutdown() {
		if(shutDownOnCountLimit.isAlive()) {
			shutDownOnCountLimit.interrupt();
		}
		super.shutdown();
	}
	//
	@Override
	public final void close()
	throws IOException {
		synchronized(remoteLoadMap) {
			if(!remoteLoadMap.isEmpty()) {
				interrupt();
				//
				LOG.debug(Markers.MSG, "log summary metrics");
				logMetrics(Markers.PERF_SUM);
				LOG.debug(Markers.MSG, "log metainfo frames");
				logMetaInfoFrames();
				//
				LoadSvc<T> nextLoadSvc;
				JMXConnector nextJMXConn;
				//
				mgmtConnExecutor.shutdownNow();
				//
				metricsReporter.close();
				//
				LOG.debug(Markers.MSG, "Closing the remote services...");
				for(final String addr : remoteLoadMap.keySet()) {
					//
					try {
						nextLoadSvc = remoteLoadMap.get(addr);
						nextLoadSvc.close();
						LOG.debug(Markers.MSG, "Server instance @ {} has been closed", addr);
					} catch(final NoSuchElementException e) {
						LOG.debug(
							Markers.ERR, "Looks like the remote load service is already shut down"
						);
					} catch(final IOException e) {
						LOG.warn(Markers.ERR, "Failed to close remote load executor service");
						LOG.trace(Markers.ERR, e.toString(), e.getCause());
					}
					//
					try {
						nextJMXConn = remoteJMXConnMap.get(addr);
						if(nextJMXConn!=null) {
							nextJMXConn.close();
							LOG.debug(Markers.MSG, "JMX connection to {} closed", addr);
						}
					} catch(final NoSuchElementException e) {
						LOG.debug(Markers.ERR, "Remote JMX connection had been interrupted earlier");
					} catch(final IOException e) {
						ExceptionHandler.trace(
							LOG, Level.WARN, e,
							String.format("Failed to close JMX connection to %s", addr)
						);
					}
					//
				}
				LoadCloseHook.del(this);
				LOG.debug(Markers.MSG, "Clear the servers map");
				remoteLoadMap.clear();
				LOG.debug(Markers.MSG, "Closed {}", getName());
			} else {
				LOG.debug(Markers.ERR, "Closed already");
			}
		}
	}
	//
	@Override
	public final long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public final Producer<T> getProducer() {
		Producer<T> producer = null;
		try {
			producer = remoteLoadMap.entrySet().iterator().next().getValue().getProducer();
		} catch(final RemoteException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to get remote producer");
		}
		return producer;
	}
	//
	@Override
	public final Map<String, LoadSvc<T>> getRemoteLoadMap() {
		return remoteLoadMap;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void handleResult(final AsyncIOTask<T> task, final AsyncIOTask.Result result)
	throws RemoteException {
		remoteLoadMap
			.get(loadSvcAddrs[(int) getTaskCount() % loadSvcAddrs.length])
			.handleResult(task, result);
	}
	//
	@Override
	public final Future<AsyncIOTask.Result> submit(final AsyncIOTask<T> request)
	throws RemoteException {
		return remoteLoadMap
			.get(loadSvcAddrs[(int) getTaskCount() % loadSvcAddrs.length])
			.submit(request);
	}
	//
	@Override
	public final void join()
	throws InterruptedException {
		join(Long.MAX_VALUE);
	}
	//
	@Override
	public final void join(final long timeOutMilliSec)
	throws InterruptedException {
		final ExecutorService joinExecutor = Executors.newFixedThreadPool(
			remoteLoadMap.size(), new WorkerFactory("joinLoadSvcWorker")
		);
		for(final String addr : remoteLoadMap.keySet()) {
			joinExecutor.submit(new RemoteJoinTask(remoteLoadMap.get(addr), timeOutMilliSec));
		}
		joinExecutor.shutdown();
		joinExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}
}
