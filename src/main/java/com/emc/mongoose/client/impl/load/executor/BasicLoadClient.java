package com.emc.mongoose.client.impl.load.executor;
//
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.api.load.model.LoadState;
import com.emc.mongoose.core.impl.load.model.BasicLoadState;
import com.emc.mongoose.core.impl.load.tasks.AwaitLoadJobTask;
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.api.load.executor.tasks.PeriodicTask;
import com.emc.mongoose.client.impl.load.executor.gauges.AvgDouble;
import com.emc.mongoose.client.impl.load.executor.gauges.MaxLong;
import com.emc.mongoose.client.impl.load.executor.gauges.MinLong;
import com.emc.mongoose.client.impl.load.executor.gauges.SumDouble;
import com.emc.mongoose.client.impl.load.executor.gauges.SumLong;
import com.emc.mongoose.client.impl.load.executor.tasks.RemoteSubmitTask;
import com.emc.mongoose.client.impl.load.executor.tasks.InterruptClientOnMaxCountTask;
import com.emc.mongoose.client.impl.load.executor.tasks.DataItemsFetchPeriodicTask;
import com.emc.mongoose.client.impl.load.executor.tasks.GaugeValuePeriodicTask;
import com.emc.mongoose.client.impl.load.executor.tasks.InterruptSvcTask;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
	//
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
	private final AtomicLong tsStart = new AtomicLong(-1);
	//
	private final ScheduledExecutorService mgmtConnExecutor;
	private final List<PeriodicTask<Collection<T>>> fetchItemsBuffTasks = new LinkedList<>();
	private final List<PeriodicTask> metricFetchTasks = new LinkedList<>();
	//////////////////////////////////////////////////////////////////////////////////////////////////
	private final long maxCount;
	private final String name, loadSvcAddrs[];
	//
	private final RunTimeConfig runTimeConfig;
	private final RequestConfig<T> reqConfigCopy;
	private final int instanceNum, metricsPeriodSec, reqTimeOutMilliSec;
	protected volatile Producer<T> producer;
	protected volatile Consumer<T> consumer = null;
	//
	public BasicLoadClient(
		final RunTimeConfig runTimeConfig, final Map<String, LoadSvc<T>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap, final RequestConfig<T> reqConfig,
		final long maxCount, final Producer<T> producer
	) {
		super(
			1, 1, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(
				(maxCount > 0 && maxCount < runTimeConfig.getRunRequestQueueSize()) ?
					(int) maxCount : runTimeConfig.getRunRequestQueueSize()
			)
		);
		setCorePoolSize(
			10 * Math.max(Runtime.getRuntime().availableProcessors(), remoteLoadMap.size())
		);
		setMaximumPoolSize(getCorePoolSize());
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
		int n = 0;
		try {
			n = remoteLoadMap.values().iterator().next().getInstanceNum();
		} catch(final NoSuchElementException | NullPointerException e) {
			LOG.error(Markers.ERR, "No remote load instances", e);
		} catch(final IOException e) {
			LOG.error(Markers.ERR, "Looks like connectivity failure", e);
		}
		instanceNum = n;
		//
		setThreadFactory(
			new GroupThreadFactory(String.format("clientSubmitWorker<%s>", name), true)
		);
		//
		this.runTimeConfig = runTimeConfig;
		try {
			this.reqConfigCopy = reqConfig.clone();
		} catch(final CloneNotSupportedException e) {
			throw new IllegalStateException("Failed to clone the request config");
		}
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.producer = producer;
		//
		metricsPeriodSec = runTimeConfig.getLoadMetricsPeriodSec();
		reqTimeOutMilliSec = runTimeConfig.getRunReqTimeOutMilliSec();
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
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to obtain MBean server connection for {}", addr
				);
			}
		}
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
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_TP, ATTR_RATE_5MIN
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
		metricFetchTasks.add(taskGetCountSubm);
		taskGetCountRej = new GaugeValuePeriodicTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_REJ, ATTR_COUNT)
		);
		metricFetchTasks.add(taskGetCountRej);
		taskGetCountSucc = new GaugeValuePeriodicTask<>(metricSuccCount);
		metricFetchTasks.add(taskGetCountSucc);
		taskGetCountFail = new GaugeValuePeriodicTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_FAIL, ATTR_COUNT)
		);
		metricFetchTasks.add(taskGetCountFail);
		/*taskGetCountNanoSec = new GaugeValuePeriodicTask<>(
			registerJmxGaugeSum(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_COUNT
			)
		);*/
		taskGetCountBytes = new GaugeValuePeriodicTask<>(metricByteCount);
		metricFetchTasks.add(taskGetCountBytes);
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
		metricFetchTasks.add(taskGetLatencyMin);
		taskGetLatencyMax = new GaugeValuePeriodicTask<>(
			registerJmxGaugeMaxLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MAX
			)
		);
		metricFetchTasks.add(taskGetLatencyMax);
		taskGetTPMean = new GaugeValuePeriodicTask<>(metricTPMean);
		metricFetchTasks.add(taskGetTPMean);
		taskGetTP1Min = new GaugeValuePeriodicTask<>(metricTP1Min);
		metricFetchTasks.add(taskGetTP1Min);
		taskGetTP5Min = new GaugeValuePeriodicTask<>(metricTP5Min);
		metricFetchTasks.add(taskGetTP5Min);
		taskGetTP15Min = new GaugeValuePeriodicTask<>(metricTP15Min);
		metricFetchTasks.add(taskGetTP15Min);
		taskGetBWMean = new GaugeValuePeriodicTask<>(metricBWMean);
		metricFetchTasks.add(taskGetBWMean);
		taskGetBW1Min = new GaugeValuePeriodicTask<>(metricBW1Min);
		metricFetchTasks.add(taskGetBW1Min);
		taskGetBW5Min = new GaugeValuePeriodicTask<>(metricBW5Min);
		metricFetchTasks.add(taskGetBW5Min);
		taskGetBW15Min = new GaugeValuePeriodicTask<>(metricBW15Min);
		metricFetchTasks.add(taskGetBW15Min);
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
		metricFetchTasks.add(taskGetLatencyMed);
		taskGetLatencyAvg = new GaugeValuePeriodicTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_AVG
			)
		);
		metricFetchTasks.add(taskGetLatencyAvg);
		//
		mgmtConnExecutor = new ScheduledThreadPoolExecutor(
			remoteLoadMap.size() + fetchItemsBuffTasks.size(),
			new GroupThreadFactory(String.format("%s-aggregator", name), true)
		);
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
			MetricRegistry.name(name, mBeanName + "." + attrName),
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
	public void postProcessDataItems() {
		//
		Collection<T> nextDataItemsBuff;
		for(final PeriodicTask<Collection<T>> nextItemsBuffFetchTask : fetchItemsBuffTasks) {
			nextDataItemsBuff = nextItemsBuffFetchTask.getLastResult();
			if(nextDataItemsBuff != null && nextDataItemsBuff.size() > 0) {
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "Got next metainfo frame: containing {} records",
						nextDataItemsBuff.size()
					);
				}
				if(consumer == null) {
					for(final T nextDataItem : nextDataItemsBuff) {
						LOG.info(Markers.DATA_LIST, nextDataItem);
					}
				} else {
					try {
						for(final T nextDataItem : nextDataItemsBuff) {
							consumer.submit(nextDataItem);
						}
					} catch(final InterruptedException e) {
						LOG.debug(Markers.MSG, "Interrupted while feeding the consumer");
						break;
					} catch(final RemoteException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to feed the data item to consumer"
						);
					}
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
			final String msg;
			if(Markers.PERF_SUM.equals(logMarker)) {
				msg = String.format(
					LogUtil.LOCALE_DEFAULT, MSG_FMT_SUM_METRICS,
					//
					name, countSucc,
					countFail == 0 ?
						Long.toString(countFail) :
						(float) countSucc / countFail > 100 ?
							String.format(LogUtil.INT_YELLOW_OVER_GREEN, countFail) :
							String.format(LogUtil.INT_RED_OVER_GREEN, countFail),
					//
					avgLat, minLat == Long.MAX_VALUE ? 0 : minLat,
					medLat, maxLat == Long.MIN_VALUE ? 0 : maxLat,
					//
					taskGetTPMean.getLastResult(), taskGetTP1Min.getLastResult(),
					taskGetTP5Min.getLastResult(), taskGetTP15Min.getLastResult(),
					//
					taskGetBWMean.getLastResult() / MIB, taskGetBW1Min.getLastResult() / MIB,
					taskGetBW5Min.getLastResult() / MIB, taskGetBW15Min.getLastResult() / MIB
				);
			} else {
				msg = String.format(
					LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
					//
					countSucc, taskGetCountSubm.getLastResult() - countSucc,
					countFail == 0 ?
						Long.toString(countFail) :
						(float) countSucc / countFail > 100 ?
							String.format(LogUtil.INT_YELLOW_OVER_GREEN, countFail) :
							String.format(LogUtil.INT_RED_OVER_GREEN, countFail),
					//
					avgLat, minLat == Long.MAX_VALUE ? 0 : minLat, medLat, maxLat == Long.MIN_VALUE ? 0 : maxLat,
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
				LogUtil.exception(LOG, Level.TRACE, e, "Terminated already");
			} else {
				LogUtil.exception(LOG, Level.TRACE, e, "Unexpected failure");
			}
		}/* catch(final TimeoutException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Distributed metrics aggregation timeout");
		}*/
	}
	//
	private void schedulePeriodicMgmtTasks() {
		LoadSvc<T> nextLoadSvc;
		final int periodSec = metricsPeriodSec > 0 ? metricsPeriodSec : 10;
		//
		for(final String loadSvcAddr : loadSvcAddrs) {
			nextLoadSvc = remoteLoadMap.get(loadSvcAddr);
			final PeriodicTask<Collection<T>> nextFrameFetchTask = new DataItemsFetchPeriodicTask<>(
				nextLoadSvc
			);
			fetchItemsBuffTasks.add(nextFrameFetchTask);
			mgmtConnExecutor.scheduleAtFixedRate(
				nextFrameFetchTask, 0, periodSec, TimeUnit.SECONDS
			);
		}
		//
		for(final PeriodicTask metricTask : metricFetchTasks) {
			mgmtConnExecutor.scheduleAtFixedRate(metricTask, 0, periodSec, TimeUnit.SECONDS);
		}
		//
		mgmtConnExecutor.scheduleAtFixedRate(
			new Runnable() {
				@Override
				public final void run() {
					postProcessDataItems();
				}
			}, 0, periodSec, TimeUnit.SECONDS
		);
		mgmtConnExecutor.scheduleAtFixedRate(
			new InterruptClientOnMaxCountTask(
				this, maxCount,
				new PeriodicTask[] {taskGetCountSucc, taskGetCountFail, taskGetCountRej}
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
	//
	@Override
	public final synchronized void start() {
		if(tsStart.compareAndSet(-1, System.nanoTime())) {
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
			//
			if(producer == null) {
				LOG.debug(Markers.MSG, "{}: using an external data items producer", getName());
			} else {
				//
				try {
					producer.start();
					LOG.debug(Markers.MSG, "Started object producer {}", producer);
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to start the producer");
				}
			}
			//
			schedulePeriodicMgmtTasks();
			metricsReporter.start();
			LoadCloseHook.add(this);
			prestartAllCoreThreads();
			//
			LOG.debug(Markers.MSG, "{}: started", name);
		} else {
			throw new IllegalStateException(name + ": was started already");
		}
	}
	//
	@Override
	public final void interrupt() {
		final int reqTimeOutMilliSec = runTimeConfig.getRunReqTimeOutMilliSec();
		//
		if(!isShutdown()) {
			LogUtil.trace(LOG, Level.DEBUG, Markers.MSG, "Interrupting {}", name);
			shutdown();
		}
		//
		if(!isTerminated()) {
			final ExecutorService interruptExecutor = Executors.newFixedThreadPool(
				remoteLoadMap.size(),
				new GroupThreadFactory(String.format("interrupt<%s>", getName()))
			);
			for(final String addr : loadSvcAddrs) {
				interruptExecutor.submit(new InterruptSvcTask(remoteLoadMap.get(addr), addr));
			}
			interruptExecutor.shutdown();
			try {
				interruptExecutor.awaitTermination(reqTimeOutMilliSec, TimeUnit.MILLISECONDS);
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Interrupting interrupted %<");
			}
			LOG.debug(Markers.MSG, "{}: interrupted", name);
		}
		//
		if(consumer != null) {
			try {
				consumer.shutdown();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to shut down the consumer \"{}\"", consumer
				);
			}
		}
	}
	//
	@Override
	public final Consumer<T> getConsumer() {
		return consumer;
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer)
	throws RemoteException {
		this.consumer = consumer;
		/*if(LoadClient.class.isInstance(consumer)) {
			LOG.debug(Markers.MSG, "Consumer is a LoadClient instance");
			// consumer is client which has the map of consumers
			// this is necessary for the distributed chain/rampup scenarios
			this.consumer = consumer;
			final Map<String, LoadSvc<T>> consumeMap = ((LoadClient<T>) consumer)
				.getRemoteLoadMap();
			for(final String addr : consumeMap.keySet()) {
				remoteLoadMap.get(addr).setConsumer(consumeMap.get(addr));
			}
		} else if(DataItemOutputConsumer.class.isInstance(consumer)) {
			// high-level API is used
			LOG.debug(Markers.MSG, "Consumer is data item output");
			this.consumer = consumer;
		} else if(LoadSvc.class.isInstance(consumer)) {
			// single consumer for all these producers
			final LoadSvc<T> loadSvc = (LoadSvc<T>) consumer;
			LOG.debug(Markers.MSG, "Consumer is a load service");
			for(final String addr : loadSvcAddrs) {
				remoteLoadMap.get(addr).setConsumer(loadSvc);
			}
		} else {
			LOG.error(
				Markers.ERR, "Unexpected consumer type: {}",
				consumer == null ? null : consumer.getClass()
			);
		}*/
	}
	//
	@Override
	public final RequestConfig<T> getRequestConfig() {
		return reqConfigCopy;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final AtomicInteger rrc = new AtomicInteger(0);
	//
	@Override
	public final void submit(final T dataItem)
	throws RejectedExecutionException, InterruptedException {
		Future remoteSubmFuture = null;
		String nextLoadSvcAddr;
		for(
			int tryCount = 0;
			tryCount < reqTimeOutMilliSec && remoteSubmFuture == null && !isShutdown();
			tryCount ++
		) {
			try {
				nextLoadSvcAddr = loadSvcAddrs[(rrc.get() + tryCount) % loadSvcAddrs.length];
				remoteSubmFuture = submit(
					RemoteSubmitTask.getInstance(remoteLoadMap.get(nextLoadSvcAddr), dataItem)
				);
				rrc.incrementAndGet();
			} catch(final RejectedExecutionException e) {
				Thread.sleep(tryCount);
			}
		}
	}
	//
	private void forceFetchAndAggregation() {
		final ExecutorService forcedAggregator = Executors.newFixedThreadPool(
			fetchItemsBuffTasks.size() + metricFetchTasks.size(),
			new GroupThreadFactory("forcedAggregator")
		);
		//
		for(final PeriodicTask<Collection<T>> frameFetchTask : fetchItemsBuffTasks) {
			forcedAggregator.submit(frameFetchTask);
		}
		for(final PeriodicTask metricFetchTask : metricFetchTasks) {
			forcedAggregator.submit(metricFetchTask);
		}
		forcedAggregator.shutdown();
		//
		try {
			forcedAggregator.awaitTermination(metricsPeriodSec, TimeUnit.SECONDS);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Interrupted while aggregating the remote info");
		} finally {
			forcedAggregator.shutdownNow();
		}
	}
	//
	@Override
	public LoadState getLoadState()
	throws RemoteException {
		forceFetchAndAggregation();
		return new BasicLoadState(
			instanceNum,
			runTimeConfig, metricSuccCount.getValue(), taskGetCountFail.getLastResult(),
			TimeUnit.NANOSECONDS, System.nanoTime() - tsStart.get()
		);
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
				forceFetchAndAggregation();
				LOG.debug(Markers.MSG, "log summary metrics");
				logMetrics(Markers.PERF_SUM);
				LOG.debug(Markers.MSG, "log metainfo frames");
				postProcessDataItems();
				LOG.debug(
					Markers.MSG, "Dropped {} remote tasks",
					shutdownNow().size() + mgmtConnExecutor.shutdownNow().size()
				);
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
						LogUtil.exception(
							LOG, Level.DEBUG, e, "No remote service found for closing"
						);
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to close remote load executor service"
						);
					}
					//
					try {
						nextJMXConn = remoteJMXConnMap.get(addr);
						if(nextJMXConn != null) {
							nextJMXConn.close();
							LOG.debug(Markers.MSG, "JMX connection to {} closed", addr);
						}
					} catch(final NoSuchElementException e) {
						LOG.debug(
							Markers.ERR, "Remote JMX connection had been interrupted earlier"
						);
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to close JMX connection to {}", addr
						);
					}
				}
				//
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
			LogUtil.exception(LOG, Level.WARN, e, "Failed to get remote producer");
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
	public final void handleResult(final IOTask<T> task)
	throws RemoteException {
		remoteLoadMap
			.get(loadSvcAddrs[(int) (getTaskCount() % loadSvcAddrs.length)])
			.handleResult(task);
	}
	//
	@Override
	public final void shutdown() {
		super.shutdown();
		LOG.debug(Markers.MSG, "{}: shutdown invoked", getName());
		try {
			awaitTermination(runTimeConfig.getRunReqTimeOutMilliSec(), TimeUnit.MILLISECONDS);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted");
		} finally {
			for(final String addr : remoteLoadMap.keySet()) {
				try {
					remoteLoadMap.get(addr).shutdown();
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to shut down remote load service");
				}
			}
		}
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
	public final void await()
	throws RemoteException, InterruptedException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
			remoteLoadMap.size() + 1,
			new GroupThreadFactory(String.format("awaitWorker<%s>", getName()))
		);
		awaitExecutor.submit(
			new Runnable() {
				@Override
				public final void run() {
					// wait the remaining tasks to be transmitted to load servers
					LOG.debug(
						Markers.MSG, "{}: waiting remaining {} tasks to complete", getName(),
						getQueue().size() + getActiveCount()
					);
					try {
						awaitTermination(timeOut, timeUnit);
					} catch(final InterruptedException e) {
						LOG.debug(Markers.MSG, "Interrupted");
					}
				}
			}
		);
		for(final String addr : remoteLoadMap.keySet()) {
			awaitExecutor.submit(new AwaitLoadJobTask(remoteLoadMap.get(addr), timeOut, timeUnit));
		}
		awaitExecutor.shutdown();
		try {
			LOG.debug(Markers.MSG, "Wait remote await tasks for finish {}[{}]", timeOut, timeUnit);
			if(awaitExecutor.awaitTermination(timeOut, timeUnit)) {
				LOG.debug(Markers.MSG, "All await tasks finished");
			} else {
				LOG.debug(Markers.MSG, "Await tasks execution timeout");
			}
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
			throw new InterruptedException();
		} finally {
			LOG.debug(
				Markers.MSG, "Interrupted await tasks: {}",
				Arrays.toString(awaitExecutor.shutdownNow().toArray())
			);
		}
	}
}
