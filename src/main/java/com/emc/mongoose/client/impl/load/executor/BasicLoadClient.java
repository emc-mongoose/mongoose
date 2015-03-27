package com.emc.mongoose.client.impl.load.executor;
//
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.ConsoleColors;
import com.emc.mongoose.common.logging.Settings;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.common.logging.TraceLogger;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.LogConsumer;
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.api.load.executor.tasks.PeriodicTask;
import com.emc.mongoose.client.api.persist.DataItemBufferClient;
import com.emc.mongoose.client.impl.load.executor.gauges.AvgDouble;
import com.emc.mongoose.client.impl.load.executor.gauges.MaxLong;
import com.emc.mongoose.client.impl.load.executor.gauges.MinLong;
import com.emc.mongoose.client.impl.load.executor.gauges.SumDouble;
import com.emc.mongoose.client.impl.load.executor.gauges.SumLong;
import com.emc.mongoose.client.impl.load.executor.tasks.CountLimitWaitTask;
import com.emc.mongoose.client.impl.load.executor.tasks.FrameFetchPeriodicTask;
import com.emc.mongoose.client.impl.load.executor.tasks.GaugeValuePeriodicTask;
import com.emc.mongoose.client.impl.load.executor.tasks.InterruptSvcTask;
import com.emc.mongoose.client.impl.load.executor.tasks.RemoteJoinTask;
import com.emc.mongoose.client.impl.load.executor.tasks.RemoteSubmitTask;
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
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 20.10.14.
 */
public class BasicLoadClient<T extends DataItem>
extends ThreadPoolExecutor
implements LoadClient<T> {
	//
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
		metricTPMean, metricTP1Min, metricTP5Min, metricTP15Min,
		metricBWMean, metricBW1Min, metricBW5Min, metricBW15Min;
	@SuppressWarnings("FieldCanBeLocal")
	private final GaugeValuePeriodicTask<Long>
		taskGetCountSubm, taskGetCountRej, taskGetCountSucc, taskGetCountFail,
		taskGetLatencyMin, taskGetLatencyMax,
		taskGetCountBytes;
	private final GaugeValuePeriodicTask<Double>
		taskGetTPMean, taskGetTP1Min, taskGetTP5Min, taskGetTP15Min,
		taskGetBWMean, taskGetBW1Min, taskGetBW5Min, taskGetBW15Min,
		taskGetLatencyMed, taskGetLatencyAvg;
	//
	private final List<PeriodicTask<List<T>>> frameFetchTasks = new ArrayList<>();
	//
	private final ScheduledExecutorService mgmtConnExecutor;
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final long maxCount;
	private final String name, loadSvcAddrs[];
	//
	private final LogConsumer<T> metaInfoLog;
	private final RunTimeConfig runTimeConfig;
	private final RequestConfig<T> reqConfig;
	private final int retryCountMax, retryDelayMilliSec, metricsPeriodSec;
	//
	public BasicLoadClient(
		final RunTimeConfig runTimeConfig, final Map<String, LoadSvc<T>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap, final RequestConfig<T> reqConfig,
		final long maxCount
	) {
		super(
			remoteLoadMap.size(), remoteLoadMap.size(), 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(
				maxCount > 0 ?
					Math.min(
						maxCount > Short.MAX_VALUE ? Short.MAX_VALUE : (int) maxCount,
						RunTimeConfig.getContext().getRunRequestQueueSize())
					:
					RunTimeConfig.getContext().getRunRequestQueueSize()
			)
		);
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
		setThreadFactory(new NamingWorkerFactory(String.format("clientSubmitWorker<%s>", name)));
		//
		this.runTimeConfig = runTimeConfig;
		this.reqConfig = reqConfig;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		//
		metricsPeriodSec = runTimeConfig.getLoadMetricsPeriodSec();
		//
		final MBeanServer mBeanServer = ServiceUtils.getMBeanServer(
			runTimeConfig.getRemotePortExport()
		);
		metricsReporter = JmxReporter.forRegistry(metrics)
			//.convertDurationsTo(TimeUnit.SECONDS)
			//.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		////////////////////////////////////////////////////////////////////////////////////////////
		this.remoteLoadMap = remoteLoadMap;
		this.loadSvcAddrs = new String[remoteLoadMap.size()];
		remoteLoadMap.keySet().toArray(this.loadSvcAddrs);
		this.remoteJMXConnMap = remoteJMXConnMap;
		////////////////////////////////////////////////////////////////////////////////////////////
		mBeanSrvConnMap = new HashMap<>();
		for(final String addr: loadSvcAddrs) {
			try {
				mBeanSrvConnMap.put(addr, remoteJMXConnMap.get(addr).getMBeanServerConnection());
			} catch(final IOException e) {
				TraceLogger.failure(
					LOG, Level.ERROR, e,
					String.format("Failed to obtain MBean server connection for %s", addr)
				);
			}
		}
		//
		mgmtConnExecutor = new ScheduledThreadPoolExecutor(
			20 + remoteLoadMap.size(), new NamingWorkerFactory(String.format("%s-remoteMonitor", name))
		) { // make the shutdown method synchronized
			@Override
			public final synchronized void shutdown() {
				super.shutdown();
			}
		};
		////////////////////////////////////////////////////////////////////////////////////////////
		metricSuccCount = registerJmxGaugeSum(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_TP, ATTR_COUNT
		);
		metricTPMean = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_TP, ATTR_RATE_MEAN
		);
		metricTP1Min = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_TP, ATTR_RATE_1MIN
		);
		metricTP5Min = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ+"."+METRIC_NAME_TP, ATTR_RATE_5MIN
		);
		metricTP15Min = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_TP, ATTR_RATE_15MIN
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
		taskGetCountSubm = new GaugeValuePeriodicTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_SUBM, ATTR_COUNT)
		);
		taskGetCountRej = new GaugeValuePeriodicTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_REJ, ATTR_COUNT)
		);
		taskGetCountSucc = new GaugeValuePeriodicTask<>(metricSuccCount);
		taskGetCountFail = new GaugeValuePeriodicTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_FAIL, ATTR_COUNT)
		);
		/*taskGetCountNanoSec = new GaugeValuePeriodicTask<>(
			registerJmxGaugeSum(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_COUNT
			)
		);*/
		taskGetCountBytes = new GaugeValuePeriodicTask<>(metricByteCount);
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
		taskGetLatencyMin = new GaugeValuePeriodicTask<>(
			registerJmxGaugeMinLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MIN
			)
		);
		taskGetLatencyMax = new GaugeValuePeriodicTask<>(
			registerJmxGaugeMaxLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MAX
			)
		);
		taskGetTPMean = new GaugeValuePeriodicTask<>(metricTPMean);
		taskGetTP1Min = new GaugeValuePeriodicTask<>(metricTP1Min);
		taskGetTP5Min = new GaugeValuePeriodicTask<>(metricTP5Min);
		taskGetTP15Min = new GaugeValuePeriodicTask<>(metricTP15Min);
		taskGetBWMean = new GaugeValuePeriodicTask<>(metricBWMean);
		taskGetBW1Min = new GaugeValuePeriodicTask<>(metricBW1Min);
		taskGetBW5Min = new GaugeValuePeriodicTask<>(metricBW5Min);
		taskGetBW15Min = new GaugeValuePeriodicTask<>(metricBW15Min);
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
		taskGetLatencyMed = new GaugeValuePeriodicTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MED
			)
		);
		taskGetLatencyAvg = new GaugeValuePeriodicTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_AVG
			)
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		metaInfoLog = new LogConsumer<>(maxCount, Math.max(2, remoteLoadMap.size()));
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private Gauge<Long> registerJmxGaugeSum(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName + "." + attrName),
			new SumLong(name, domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	//
	private Gauge<Long> registerJmxGaugeMinLong(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new MinLong(name, domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	//
	private Gauge<Long> registerJmxGaugeMaxLong(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new MaxLong(name, domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	//
	private Gauge<Double> registerJmxGaugeSumDouble(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new SumDouble(name, domain, mBeanName, attrName, mBeanSrvConnMap)
		);
	}
	//
	private Gauge<Double> registerJmxGaugeAvgDouble(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName + "." + attrName),
			new AvgDouble(name, domain, mBeanName, attrName, mBeanSrvConnMap)
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
	@Override
	public void logMetaInfoFrames() {
		//
		List<T> nextMetaInfoFrame;
		for(final PeriodicTask<List<T>> nextFrameFetchTask : frameFetchTasks) {
			nextMetaInfoFrame = nextFrameFetchTask.getLastResult();
			try {
				if(nextMetaInfoFrame != null && nextMetaInfoFrame.size() > 0) {
					LOG.trace(Markers.MSG, "Got next metainfo frame: {}", nextMetaInfoFrame);
					for(final T nextMetaInfoRec: nextMetaInfoFrame) {
						metaInfoLog.submit(nextMetaInfoRec);
					}
				}
			} catch(final InterruptedException | RejectedExecutionException e) {
				if(LOG.isTraceEnabled(Markers.ERR)) {
					TraceLogger.failure(LOG, Level.TRACE, e, "Failed to log the metainfo frame");
				}
			}
		}
	}
	//
	@Override
	public final void logMetrics(final Marker logMarker) {
		try {
			final long
				countSucc = taskGetCountSucc.getLastResult(),
				countFail = taskGetCountFail.getLastResult(),
				avgLat = taskGetLatencyAvg.getLastResult().intValue(),
				minLat = taskGetLatencyMin.getLastResult(),
				medLat = taskGetLatencyMed.getLastResult().intValue(),
				maxLat = taskGetLatencyMax.getLastResult();
			final boolean isSummary = Markers.PERF_SUM.equals(logMarker);
			final String msg;
			if(isSummary) {
				msg = String.format(
					Settings.LOCALE_DEFAULT, MSG_FMT_SUM_METRICS,
					//
					name, countSucc,
					countFail == 0 ?
						Long.toString(countFail) :
						(float) countSucc / countFail > 100 ?
							String.format(ConsoleColors.INT_YELLOW_OVER_GREEN, countFail) :
							String.format(ConsoleColors.INT_RED_OVER_GREEN, countFail),
					//
					avgLat, minLat, medLat, maxLat,
					//
					taskGetTPMean.getLastResult(), taskGetTP1Min.getLastResult(),
					taskGetTP5Min.getLastResult(), taskGetTP15Min.getLastResult(),
					//
					taskGetBWMean.getLastResult() / MIB, taskGetBW1Min.getLastResult() / MIB,
					taskGetBW5Min.getLastResult() / MIB, taskGetBW15Min.getLastResult() / MIB
				);
			} else {
				msg = String.format(
					Settings.LOCALE_DEFAULT, MSG_FMT_METRICS,
					//
					countSucc, taskGetCountSubm.getLastResult() - countSucc,
					countFail == 0 ?
						Long.toString(countFail) :
						(float) countSucc / countFail > 100 ?
							String.format(ConsoleColors.INT_YELLOW_OVER_GREEN, countFail) :
							String.format(ConsoleColors.INT_RED_OVER_GREEN, countFail),
					//
					avgLat, minLat, medLat, maxLat,
					//
					taskGetTPMean.getLastResult(), taskGetTP1Min.getLastResult(),
					taskGetTP5Min.getLastResult(), taskGetTP15Min.getLastResult(),
					//
					taskGetBWMean.getLastResult() / MIB, taskGetBW1Min.getLastResult() / MIB,
					taskGetBW5Min.getLastResult() / MIB, taskGetBW15Min.getLastResult() / MIB
				);
			}
			LOG.info(logMarker, msg);
		} catch(final NullPointerException e) {
			if(isTerminating() || isTerminated()) {
				TraceLogger.failure(LOG, Level.TRACE, e, "Unexpected failure");
			} else {
				TraceLogger.failure(LOG, Level.WARN, e, "Unexpected failure");
			}
		}/* catch(final TimeoutException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Distributed metrics aggregation timeout");
		}*/
	}
	//
	private void schedulePeriodicMgmtTasks() {
		LoadSvc<T> nextLoadSvc;
		final int periodSec = metricsPeriodSec > 0 ? metricsPeriodSec : 10;
		synchronized(mgmtConnExecutor) {
			//
			for(final String loadSvcAddr : loadSvcAddrs) {
				nextLoadSvc = remoteLoadMap.get(loadSvcAddr);
				final PeriodicTask<List<T>> nextFrameFetchTask = new FrameFetchPeriodicTask<>(
					nextLoadSvc
				);
				frameFetchTasks.add(nextFrameFetchTask);
				mgmtConnExecutor.scheduleAtFixedRate(
					nextFrameFetchTask, 0, periodSec, TimeUnit.SECONDS
				);
			}
			//
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetCountSubm, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetCountRej, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetCountSucc, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetCountFail, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetCountBytes, 0, periodSec, TimeUnit.SECONDS
			);
			//
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetLatencyAvg, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetLatencyMin, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetLatencyMed, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetLatencyMax, 0, periodSec, TimeUnit.SECONDS
			);
			//
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetTPMean, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetTP1Min, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetTP5Min, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetTP15Min, 0, periodSec, TimeUnit.SECONDS
			);
			//
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetBWMean, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetBW1Min, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetBW5Min, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				taskGetBW15Min, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public final void run() {
						logMetaInfoFrames();
					}
				}, 0, periodSec, TimeUnit.SECONDS
			);
			mgmtConnExecutor.scheduleAtFixedRate(
				new CountLimitWaitTask(
					this, maxCount,
					new PeriodicTask[] { taskGetCountSucc, taskGetCountRej, taskGetCountRej }
				), 0, periodSec, TimeUnit.SECONDS
			);
			//
			if(metricsPeriodSec > 0) {
				mgmtConnExecutor.scheduleAtFixedRate(
					new Runnable() {
						@Override
						public final void run() {
							logMetrics(Markers.PERF_AVG);
						}
					}, 0, metricsPeriodSec, TimeUnit.SECONDS
				);
			}
		}
	}
	//
	@Override
	public final void start() {
		LoadSvc<T> nextLoadSvc;
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
		schedulePeriodicMgmtTasks();
		metricsReporter.start();
		LoadCloseHook.add(this);
		prestartAllCoreThreads();
		//
		LOG.debug(Markers.MSG, "{}: started", name);
	}
	//
	@Override
	public final void interrupt() {
		final int reqTimeOutMilliSec = runTimeConfig.getRunReqTimeOutMilliSec();
		//
		if(!isShutdown()) {
			LOG.debug(Markers.MSG, "{}: interrupting...", name);
			shutdown();
			final ExecutorService interruptExecutor = Executors.newFixedThreadPool(
				remoteLoadMap.size(), new NamingWorkerFactory(String.format("interrupt<%s>", getName()))
			);
			for(final String addr : loadSvcAddrs) {
				interruptExecutor.submit(new InterruptSvcTask(remoteLoadMap.get(addr), addr));
			}
			interruptExecutor.shutdown();
			try {
				interruptExecutor.awaitTermination(reqTimeOutMilliSec, TimeUnit.MILLISECONDS);
			} catch(final InterruptedException e) {
				TraceLogger.failure(LOG, Level.DEBUG, e, "Interrupting interrupted %<");
			}
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
				TraceLogger.failure(LOG, Level.WARN, e, "Data item class mismatch");
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
				TraceLogger.failure(LOG, Level.WARN, e, "Data item class mismatch");
			}
		} else if(DataItemBufferClient.class.isInstance(consumer)) {
			try {
				final DataItemBufferClient<T> mediator = (DataItemBufferClient<T>) consumer;
				LOG.debug(Markers.MSG, "Consumer is remote mediator buffer");
				for(final String addr : loadSvcAddrs) {
					remoteLoadMap.get(addr).setConsumer(mediator.get(addr));
				}
			} catch(final ClassCastException e) {
				TraceLogger.failure(LOG, Level.WARN, e, "Data item class mismatch");
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
	protected final AtomicLong countSubmCalls = new AtomicLong(0);
	//
	@Override
	public final void submit(final T dataItem)
	throws InterruptedException {
		if(maxCount > getTaskCount()) { // getTaskCount() is inaccurate
			if(dataItem == null) {
				LOG.debug(Markers.MSG, "{}: poison submitted, performing the shutdown", name);
				// poison the remote load service instances
				for(final String addr : loadSvcAddrs) {
					try {
						remoteLoadMap.get(addr).submit((T) null); // feed the poison
					} catch(final Exception e) {
						TraceLogger.failure(
							LOG, Level.WARN, e,
							String.format("Failed to submit the poison to @%s", addr)
						);
					}
				}
				//
				shutdown();
			} else {
				final String addr = loadSvcAddrs[(int) (countSubmCalls.get() % loadSvcAddrs.length)];
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
	public final void close()
	throws IOException {
		LOG.debug(Markers.MSG, "trying to close");
		synchronized(remoteLoadMap) {
			if(!remoteLoadMap.isEmpty()) {
				LOG.debug(Markers.MSG, "do performing close");
				interrupt();
				mgmtConnExecutor.shutdownNow();
				LOG.debug(Markers.MSG, "log summary metrics");
				logMetrics(Markers.PERF_SUM);
				LOG.debug(Markers.MSG, "log metainfo frames");
				logMetaInfoFrames();
				metricsReporter.close();
				//
				LOG.debug(Markers.MSG, "Closing the remote services...");
				LoadSvc<T> nextLoadSvc;
				JMXConnector nextJMXConn;
				for(final String addr : remoteLoadMap.keySet()) {
					//
					try {
						nextLoadSvc = remoteLoadMap.get(addr);
						nextLoadSvc.close();
						LOG.debug(Markers.MSG, "Server instance @ {} has been closed", addr);
					} catch(final NoSuchElementException e) {
						if(!isTerminating() && !isTerminated()) {
							LOG.debug(
								Markers.ERR,
								"Looks like the remote load service is already shut down"
							);
						}
					} catch(final NoSuchObjectException e) {
						TraceLogger.failure(
							LOG, Level.DEBUG, e, "No remote service found for closing"
						);
					} catch(final IOException e) {
						TraceLogger.failure(
							LOG, Level.WARN, e, "Failed to close remote load executor service"
						);
					}
					//
					try {
						nextJMXConn = remoteJMXConnMap.get(addr);
						if(nextJMXConn!=null) {
							nextJMXConn.close();
							LOG.debug(Markers.MSG, "JMX connection to {} closed", addr);
						}
					} catch(final NoSuchElementException e) {
						LOG.debug(
							Markers.ERR, "Remote JMX connection had been interrupted earlier"
						);
					} catch(final IOException e) {
						TraceLogger.failure(
							LOG, Level.WARN, e,
							String.format("Failed to close JMX connection to %s", addr)
						);
					}
				}
				//
				metaInfoLog.close();
				LOG.debug(Markers.MSG, "Metainfo logger closed");
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
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to get remote producer");
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
	public final void handleResult(final IOTask<T> task, final IOTask.Status status)
	throws RemoteException {
		remoteLoadMap
			.get(loadSvcAddrs[(int) (getTaskCount() % loadSvcAddrs.length)])
			.handleResult(task, status);
	}
	//
	@Override
	public final Future<IOTask.Status> submit(final IOTask<T> request)
	throws RemoteException {
		return remoteLoadMap
			.get(loadSvcAddrs[(int) (getTaskCount() % loadSvcAddrs.length)])
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
			remoteLoadMap.size(), new NamingWorkerFactory(String.format("joinWorker<%s>", getName()))
		);
		for(final String addr : remoteLoadMap.keySet()) {
			joinExecutor.submit(new RemoteJoinTask(remoteLoadMap.get(addr), timeOutMilliSec));
		}
		joinExecutor.shutdown();
		joinExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}
}
