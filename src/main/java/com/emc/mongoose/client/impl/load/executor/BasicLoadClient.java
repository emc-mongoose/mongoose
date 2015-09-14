package com.emc.mongoose.client.impl.load.executor;
//
import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Clock;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
// mongoose-common.jar
import com.emc.mongoose.client.impl.load.executor.gauges.AvgDouble;
import com.emc.mongoose.client.impl.load.executor.gauges.MaxLong;
import com.emc.mongoose.client.impl.load.executor.gauges.MinLong;
import com.emc.mongoose.client.impl.load.executor.gauges.SumDouble;
import com.emc.mongoose.client.impl.load.executor.gauges.SumLong;
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.api.load.model.LoadState;
import com.emc.mongoose.core.impl.data.model.NewDataItemInput;
import com.emc.mongoose.core.impl.load.model.BasicLoadState;
import com.emc.mongoose.core.impl.load.model.DataItemInputProducer;
import com.emc.mongoose.core.impl.load.model.metrics.ResumableUserTimeClock;
import com.emc.mongoose.core.impl.load.tasks.AwaitLoadJobTask;
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.api.load.executor.tasks.PeriodicTask;
import com.emc.mongoose.client.impl.load.executor.tasks.InterruptClientOnMaxCountTask;
import com.emc.mongoose.client.impl.load.executor.tasks.DataItemsFetchPeriodicTask;
import com.emc.mongoose.client.impl.load.executor.tasks.CachedGaugePeriodicTask;
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
	private final Clock clock = new ResumableUserTimeClock();
	private final MetricRegistry metrics = new MetricRegistry();
	protected final JmxReporter metricsReporter;
	//
	@SuppressWarnings("FieldCanBeLocal")
	private final CachedGauge<Long>
		metricSuccCount, metricByteCount;
	@SuppressWarnings("FieldCanBeLocal")
	private final CachedGauge<Double>
		metricTPMean, metricTPLast, metricBWMean, metricBWLast;
	@SuppressWarnings("FieldCanBeLocal")
	private final CachedGaugePeriodicTask<Long>
		taskGetCountSubm, taskGetCountRej, taskGetCountSucc, taskGetCountFail,
		taskGetDurationMin, taskGetDurationMax,
		taskGetLatencyMin, taskGetLatencyMax,
		taskGetCountBytes;
	private final CachedGaugePeriodicTask<Double>
		taskGetTPMean, taskGetTPLast,
		taskGetBWMean, taskGetBWLast,
		taskGetDurationStdDev, taskGetDurationAvg,
		taskGetLatencyStdDev, taskGetLatencyAvg;
	private final AtomicLong tsStart = new AtomicLong(-1);
	//
	private final ScheduledExecutorService mgmtConnExecutor;
	private final List<PeriodicTask<Collection<T>>> fetchItemsBuffTasks = new LinkedList<>();
	private final List<PeriodicTask> metricFetchTasks = new LinkedList<>();
	private volatile long durationValues[] = null, latencyValues[] = null;
	//////////////////////////////////////////////////////////////////////////////////////////////////
	private final long maxCount;
	private final String name, loadSvcAddrs[];
	//
	private final RunTimeConfig runTimeConfig;
	private final RequestConfig<T> reqConfigCopy;
	private final int instanceNum, metricsPeriodSec;
	protected volatile Producer<T> producer;
	protected volatile Consumer<T> consumer = null;
	//
	public BasicLoadClient(
		final RunTimeConfig runTimeConfig, final Map<String, LoadSvc<T>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap, final RequestConfig<T> reqConfig,
		final long maxCount, final DataItemInput<T> itemSrc
	) {
		super(
			1, 1, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(
				(maxCount > 0 && maxCount < runTimeConfig.getTasksMaxQueueSize()) ?
					(int) maxCount : runTimeConfig.getTasksMaxQueueSize()
			)
		);
		setCorePoolSize(
			remoteLoadMap.size() * Math.max(1, Runtime.getRuntime().availableProcessors())
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
		//
		if(itemSrc != null && !NewDataItemInput.class.isInstance(itemSrc)) {
			producer = new DataItemInputProducer<>(itemSrc);
			try {
				producer.setConsumer(this);
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Unexpected failure");
			}
		}
		//
		metricsPeriodSec = runTimeConfig.getLoadMetricsPeriodSec();
		//
		if(runTimeConfig.getFlagServeIfNotLoadServer()) {
			final MBeanServer mBeanServer = ServiceUtils.getMBeanServer(
				runTimeConfig.getRemotePortExport()
			);
			metricsReporter = JmxReporter.forRegistry(metrics)
				.registerWith(mBeanServer)
				.build();
		} else {
			metricsReporter = null;
		}
		////////////////////////////////////////////////////////////////////////////////////////////
		this.remoteLoadMap = remoteLoadMap;
		this.loadSvcAddrs = new String[remoteLoadMap.size()];
		remoteLoadMap.keySet().toArray(this.loadSvcAddrs);
		this.remoteJMXConnMap = remoteJMXConnMap;
		////////////////////////////////////////////////////////////////////////////////////////////
		mBeanSrvConnMap = new HashMap<>();
		JMXConnector jmxConnector;
		for(final String addr: loadSvcAddrs) {
			jmxConnector = remoteJMXConnMap.get(addr);
			if(jmxConnector != null) {
				try {
					mBeanSrvConnMap.put(addr, jmxConnector.getMBeanServerConnection());
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to obtain MBean server connection for {}", addr
					);
				}
			} else {
				LOG.warn(Markers.ERR, "No JMX connection to {}", addr);
			}
		}
		////////////////////////////////////////////////////////////////////////////////////////////
		metricSuccCount = registerJmxGaugeSum(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_TP, ATTR_COUNT
		);
		metricTPMean = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_TP, ATTR_RATE_MEAN
		);
		metricTPLast = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_TP, ATTR_RATE_1MIN
		);
		metricByteCount = registerJmxGaugeSum(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_BW, ATTR_COUNT
		);
		metricBWMean = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_BW, ATTR_RATE_MEAN
		);
		metricBWLast = registerJmxGaugeSumDouble(
			DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_BW, ATTR_RATE_1MIN
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		taskGetCountSubm = new CachedGaugePeriodicTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_SUBM, ATTR_COUNT)
		);
		metricFetchTasks.add(taskGetCountSubm);
		taskGetCountRej = new CachedGaugePeriodicTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_REJ, ATTR_COUNT)
		);
		metricFetchTasks.add(taskGetCountRej);
		taskGetCountSucc = new CachedGaugePeriodicTask<>(metricSuccCount);
		metricFetchTasks.add(taskGetCountSucc);
		taskGetCountFail = new CachedGaugePeriodicTask<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_FAIL, ATTR_COUNT)
		);
		metricFetchTasks.add(taskGetCountFail);
		taskGetCountBytes = new CachedGaugePeriodicTask<>(metricByteCount);
		metricFetchTasks.add(taskGetCountBytes);
		taskGetLatencyMin = new CachedGaugePeriodicTask<>(
			registerJmxGaugeMinLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MIN
			)
		);
		metricFetchTasks.add(taskGetLatencyMin);
		taskGetDurationMin = new CachedGaugePeriodicTask<>(
			registerJmxGaugeMinLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MIN
			)
		);
		metricFetchTasks.add(taskGetDurationMin);
		taskGetLatencyMax = new CachedGaugePeriodicTask<>(
			registerJmxGaugeMaxLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MAX
			)
		);
		metricFetchTasks.add(taskGetLatencyMax);
		taskGetDurationMax = new CachedGaugePeriodicTask<>(
			registerJmxGaugeMaxLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MAX
			)
		);
		metricFetchTasks.add(taskGetDurationMax);
		taskGetTPMean = new CachedGaugePeriodicTask<>(metricTPMean);
		metricFetchTasks.add(taskGetTPMean);
		taskGetTPLast = new CachedGaugePeriodicTask<>(metricTPLast);
		metricFetchTasks.add(taskGetTPLast);
		taskGetBWMean = new CachedGaugePeriodicTask<>(metricBWMean);
		metricFetchTasks.add(taskGetBWMean);
		taskGetBWLast = new CachedGaugePeriodicTask<>(metricBWLast);
		metricFetchTasks.add(taskGetBWLast);
		//
		taskGetDurationStdDev = new CachedGaugePeriodicTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MED
			)
		);
		metricFetchTasks.add(taskGetDurationStdDev);
		taskGetDurationAvg = new CachedGaugePeriodicTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_AVG
			)
		);
		metricFetchTasks.add(taskGetDurationAvg);
		//
		taskGetLatencyStdDev = new CachedGaugePeriodicTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_MED
			)
		);
		metricFetchTasks.add(taskGetLatencyStdDev);
		taskGetLatencyAvg = new CachedGaugePeriodicTask<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_LAT, ATTR_AVG
			)
		);
		metricFetchTasks.add(taskGetLatencyAvg);
		//
		mgmtConnExecutor = new ScheduledThreadPoolExecutor(
			loadSvcAddrs.length + metricFetchTasks.size() + 3,
			new GroupThreadFactory(String.format("%s-aggregator", name), true)
		);
		//
		LoadCloseHook.add(this);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private CachedGauge<Long> registerJmxGaugeSum(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName + "." + attrName),
			new SumLong(
				name, domain, mBeanName, attrName, mBeanSrvConnMap,
				clock, metricsPeriodSec, TimeUnit.SECONDS
			)
		);
	}
	//
	private CachedGauge<Long> registerJmxGaugeMinLong(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName + "." + attrName),
			new MinLong(
				name, domain, mBeanName, attrName, mBeanSrvConnMap,
				clock, metricsPeriodSec, TimeUnit.SECONDS
			)
		);
	}
	//
	private CachedGauge<Long> registerJmxGaugeMaxLong(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new MaxLong(
				name, domain, mBeanName, attrName, mBeanSrvConnMap,
				clock, metricsPeriodSec, TimeUnit.SECONDS
			)
		);
	}
	//
	private CachedGauge<Double> registerJmxGaugeSumDouble(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName+"."+attrName),
			new SumDouble(
				name, domain, mBeanName, attrName, mBeanSrvConnMap,
				clock, metricsPeriodSec, TimeUnit.SECONDS
			)
		);
	}
	//
	private CachedGauge<Double> registerJmxGaugeAvgDouble(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(name, mBeanName + "." + attrName),
			new AvgDouble(
				name, domain, mBeanName, attrName, mBeanSrvConnMap,
				clock, metricsPeriodSec, TimeUnit.SECONDS
			)
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
					} catch(final RemoteException | RejectedExecutionException e) {
						LogUtil.exception(
							LOG, Level.DEBUG, e,
							"Failed to submit all {} data items to the consumer {}",
							nextDataItemsBuff.size(), consumer
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
				avgDur = taskGetDurationAvg.getLastResult().intValue(),
				minDur = taskGetDurationMin.getLastResult(),
				stdDevDur = taskGetDurationStdDev.getLastResult().intValue(),
				maxDur = taskGetDurationMax.getLastResult(),
				avgLat = taskGetLatencyAvg.getLastResult().intValue(),
				minLat = taskGetLatencyMin.getLastResult(),
				stdDevLat = taskGetLatencyStdDev.getLastResult().intValue(),
				maxLat = taskGetLatencyMax.getLastResult();
			final String msg;
			if(Markers.PERF_SUM.equals(logMarker)) {
				msg = "\"" + name + "\" summary: " + String.format(
					LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
					//
					countSucc,
					countFail == 0 ?
						Long.toString(countFail) :
						(float) countSucc / countFail > 100 ?
							String.format(LogUtil.INT_YELLOW_OVER_GREEN, countFail) :
							String.format(LogUtil.INT_RED_OVER_GREEN, countFail),
					//
					avgDur, minDur == Long.MAX_VALUE ? 0 : minDur,
					stdDevDur, maxDur == Long.MIN_VALUE ? 0 : maxDur,
					avgLat, minLat == Long.MAX_VALUE ? 0 : minLat,
					stdDevLat, maxLat == Long.MIN_VALUE ? 0 : maxLat,
					//
					taskGetTPMean.getLastResult(), taskGetTPLast.getLastResult(),
					taskGetBWMean.getLastResult() / MIB, taskGetBWLast.getLastResult() / MIB
				);
			} else {
				msg = String.format(
					LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
					//
					countSucc,
					countFail == 0 ?
						Long.toString(countFail) :
						(float) countSucc / countFail > 100 ?
							String.format(LogUtil.INT_YELLOW_OVER_GREEN, countFail) :
							String.format(LogUtil.INT_RED_OVER_GREEN, countFail),
					//
					avgDur, minDur == Long.MAX_VALUE ? 0 : minDur,
					stdDevDur, maxDur == Long.MIN_VALUE ? 0 : maxDur,
					avgLat, minLat == Long.MAX_VALUE ? 0 : minLat,
					stdDevLat, maxLat == Long.MIN_VALUE ? 0 : maxLat,
					//
					taskGetTPMean.getLastResult(), taskGetTPLast.getLastResult(),
					taskGetBWMean.getLastResult() / MIB, taskGetBWLast.getLastResult() / MIB
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
		final int periodMilliSec = metricsPeriodSec > 0 ? 1000 * metricsPeriodSec : 1000;
		//
		for(final String loadSvcAddr : loadSvcAddrs) {
			nextLoadSvc = remoteLoadMap.get(loadSvcAddr);
			final PeriodicTask<Collection<T>> nextFrameFetchTask = new DataItemsFetchPeriodicTask<>(
				nextLoadSvc
			);
			fetchItemsBuffTasks.add(nextFrameFetchTask);
			mgmtConnExecutor.scheduleAtFixedRate(
				nextFrameFetchTask, 0, periodMilliSec, TimeUnit.MILLISECONDS
			);
		}
		//
		for(final PeriodicTask metricTask : metricFetchTasks) {
			mgmtConnExecutor.scheduleAtFixedRate(
				metricTask, 0, periodMilliSec, TimeUnit.MILLISECONDS
			);
		}
		//
		mgmtConnExecutor.scheduleAtFixedRate(
			new Runnable() {
				@Override
				public final void run() {
					postProcessDataItems();
				}
			}, 123, periodMilliSec, TimeUnit.MILLISECONDS
		);
		mgmtConnExecutor.scheduleAtFixedRate(
			new InterruptClientOnMaxCountTask(
				this, maxCount,
				new PeriodicTask[] {taskGetCountSucc, taskGetCountFail, taskGetCountRej}
			), 456, 1000, TimeUnit.MILLISECONDS
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
	@SuppressWarnings("unchecked")
	public final synchronized void start() {
		if(tsStart.compareAndSet(-1, System.nanoTime())) {
			if (runTimeConfig.isRunResumeEnabled()) {
				if (!RESTORED_STATES_MAP.containsKey(runTimeConfig.getRunId())) {
					BasicLoadState.restoreScenarioState(runTimeConfig);
				}
				setLoadState(BasicLoadState.findStateByLoadNumber(instanceNum, runTimeConfig));
			}
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
			if(metricsReporter != null) {
				metricsReporter.start();
			}
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
		if(!isShutdown()) {
			LOG.debug(Markers.MSG, "Interrupting {}", name);
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
				interruptExecutor.awaitTermination(metricsPeriodSec, TimeUnit.SECONDS);
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
		//
		LOG.debug(
			Markers.MSG, "{}: dropped {} remote tasks",
			getName(), shutdownNow().size() + mgmtConnExecutor.shutdownNow().size()
		);
		loadLastDurationAndLatencyValues();
		forceFetchAndAggregation();
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
		if(LoadClient.class.isInstance(consumer)) {
			LOG.debug(Markers.MSG, "Consumer is a LoadClient instance");
			// consumer is client which has the map of consumers
			// this is necessary for the distributed chain/rampup scenarios
			this.consumer = consumer;
			final Map<String, LoadSvc<T>> consumeMap = ((LoadClient<T>) consumer)
				.getRemoteLoadMap();
			for(final String addr : consumeMap.keySet()) {
				remoteLoadMap.get(addr).setConsumer(consumeMap.get(addr));
			}
		} else if(LoadSvc.class.isInstance(consumer)) {
			// single consumer for all these producers
			final LoadSvc<T> loadSvc = (LoadSvc<T>) consumer;
			LOG.debug(Markers.MSG, "Consumer is a load service");
			for(final String addr : loadSvcAddrs) {
				remoteLoadMap.get(addr).setConsumer(loadSvc);
			}
		} else {
			this.consumer = consumer;
		}
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
	private final class RemoteSubmitTask
	implements Runnable {
		//
		private final T dataItem;
		//
		private RemoteSubmitTask(final T dataItem) {
			this.dataItem = dataItem;
		}
		//
		@Override
		public final void run() {
			String loadSvcAddr;
			for(int tryCount = 0; tryCount < Short.MAX_VALUE && !isTerminated(); tryCount ++) {
				try {
					loadSvcAddr = loadSvcAddrs[(rrc.get() + tryCount) % loadSvcAddrs.length];
					remoteLoadMap.get(loadSvcAddr).submit(dataItem);
					rrc.incrementAndGet();
					break;
				} catch(final RejectedExecutionException | RemoteException e) {
					try {
						Thread.sleep(tryCount);
					} catch(final InterruptedException ee) {
						break;
					}
				} catch(final InterruptedException e) {
					break;
				}
			}
		}
	}
	//
	@Override
	public final void submit(final T dataItem)
	throws RejectedExecutionException, InterruptedException {
		submit(new RemoteSubmitTask(dataItem));
	}
	//
	private void forceFetchAndAggregation() {
		final ExecutorService forcedAggregator = Executors.newFixedThreadPool(
			10, new GroupThreadFactory("forcedAggregator")
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
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while aggregating the remote info");
		} finally {
			forcedAggregator.shutdownNow();
			postProcessDataItems();
		}
	}
	//
	@Override
	public void setLoadState(final LoadState<T> state) {
		if (state != null) {
			LOG.warn(Markers.MSG, "Failed to resume run in distributed mode. See #JIRA-411");
		}
	}
	//
	@Override
	@SuppressWarnings("unchecked")
	public LoadState<T> getLoadState()
	throws RemoteException {
		//forceFetchAndAggregation();
		final LoadState.Builder<T, BasicLoadState<T>> stateBuilder
			= new BasicLoadState.Builder<>();
		stateBuilder
			.setLoadNumber(instanceNum)
			.setRunTimeConfig(runTimeConfig)
			.setCountSucc(metricSuccCount.getValue())
			.setCountFail(taskGetCountFail.getLastResult())
			.setCountBytes(taskGetCountBytes.getLastResult())
			.setCountSubm(taskGetCountSubm.getLastResult())
			.setLoadElapsedTimeValue(System.nanoTime() - tsStart.get())
			.setLoadElapsedTimeUnit(TimeUnit.NANOSECONDS)
			.setDurationValues(durationValues)
			.setLatencyValues(latencyValues);
        //
		return stateBuilder.build();
	}
	//
	@Override
	public final void close()
	throws IOException {
		LOG.debug(Markers.MSG, "{}: trying to close", getName());
		synchronized(remoteLoadMap) {
			if(!remoteLoadMap.isEmpty()) {
				interrupt();
				logMetrics(Markers.PERF_SUM);
				if(metricsReporter != null) {
					metricsReporter.close();
				}
				//
				LOG.debug(Markers.MSG, "{}: closing the remote services...", getName());
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
								"Looks like the remote load service is already closed"
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
				LOG.debug(Markers.MSG, "{}: closed", getName());
			} else {
				LOG.debug(Markers.MSG, "{}: closed already", getName());
			}
		}
	}
	//
	private void loadLastDurationAndLatencyValues() {
		try {
			for(final LoadSvc<T> loadSvc : remoteLoadMap.values()) {
				final LoadState nextLoadSvcState = loadSvc.getLoadState();
				durationValues = nextLoadSvcState.getDurationValues();
				latencyValues = nextLoadSvcState.getLatencyValues();
			}
		} catch (final RemoteException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Unexpected failure");
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
		//
		final long timeOut = runTimeConfig.getLoadLimitTimeValue();
		final TimeUnit timeUnit = runTimeConfig.getLoadLimitTimeUnit();
		try {
			if(
				!awaitTermination(
					timeOut > 0 ? timeOut : Long.MAX_VALUE,
					timeUnit == null ? TimeUnit.DAYS : timeUnit
				)
			) {
				LOG.debug(
					Markers.ERR,
					"Timeout while submitting all the remaining data items to the load servers"
				);
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted");
		} finally {
			LOG.debug(
				Markers.MSG, "Submitted {} items to the load servers", getCompletedTaskCount()
			);
			for(final String addr : remoteLoadMap.keySet()) {
				try {
					remoteLoadMap.get(addr).shutdown();
				} catch(final NoSuchObjectException ignored) {
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to shut down remote load service"
					);
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
