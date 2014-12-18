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
import com.emc.mongoose.base.load.client.impl.tasks.CountLimitWaitTask;
import com.emc.mongoose.base.load.client.impl.tasks.FrameFetchTask;
import com.emc.mongoose.base.load.client.impl.tasks.GaugeValueTask;
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
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 20.10.14.
 */
public class BasicLoadClient<T extends DataItem>
extends Thread
implements LoadClient<T> {
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final Map<String, LoadSvc<T>> remoteLoadMap;
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final Map<String, JMXConnector> remoteJMXConnMap;
	private final Map<String, MBeanServerConnection> mBeanSrvConnMap;
	private final MetricRegistry metrics = new MetricRegistry();
	private final static String
		DEFAULT_DOMAIN = "metrics",
		FMT_MSG_FAIL_FETCH_VALUE = "Failed to fetch the value for \"%s\" from %s";
	protected final JmxReporter metricsReporter;
	//
	private final static String
		KEY_NAME = "name",
	//
	ATTR_COUNT = "Count",
		ATTR_MIN = "Min",
		ATTR_AVG = "Mean",
		ATTR_MAX = "Max",
		ATTR_MED = "50thPercentile",
		ATTR_75P = "75thPercentile",
		ATTR_95P = "95thPercentile",
		ATTR_98P = "98thPercentile",
		ATTR_99P = "99thPercentile",
		ATTR_RATE_MEAN = "MeanRate",
		ATTR_RATE_1MIN = "OneMinuteRate",
		ATTR_RATE_5MIN = "FiveMinuteRate",
		ATTR_RATE_15MIN = "FifteenMinuteRate";
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	private volatile long maxCount;
	//
	protected final ThreadPoolExecutor submitExecutor, mgmtConnExecutor;
	private final LogConsumer<T> metaInfoLog;
	private final RunTimeConfig runTimeConfig;
	private final RequestConfig<T> reqConfig;
	private final int retryCountMax, retryDelayMilliSec;
	//
	private final Lock lock = new ReentrantLock();
	private final Condition condDone = lock.newCondition();
	//
	public BasicLoadClient(
		final RunTimeConfig runTimeConfig,
		final Map<String, LoadSvc<T>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap,
		final RequestConfig<T> reqConfig,
		final long maxCount, final int threadCountPerServer
	) {
		//
		this.runTimeConfig = runTimeConfig;
		this.reqConfig = reqConfig;
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
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
		this.remoteJMXConnMap = remoteJMXConnMap;
		//
		try {
			final Object remoteLoads[] = remoteLoadMap.values().toArray();
			this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
			setName(LoadSvc.class.cast(remoteLoads[0]).getName()+'x'+remoteLoads.length);
		} catch(final NoSuchElementException | NullPointerException e) {
			LOG.error(Markers.ERR, "No remote load instances", e);
		} catch(final IOException e) {
			LOG.error(Markers.ERR, "Looks like connectivity failure", e);
		}
		////////////////////////////////////////////////////////////////////////////////////////////
		mBeanSrvConnMap = new HashMap<>();
		for(final String addr: remoteJMXConnMap.keySet()) {
			try {
				mBeanSrvConnMap.put(addr, remoteJMXConnMap.get(addr).getMBeanServerConnection());
			} catch(final IOException e) {
				ExceptionHandler.trace(
					LOG, Level.ERROR, e,
					String.format("Failed to obtain MBean server connection for %s", addr)
				);
			}
		}
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
		taskGetTPMean = new GaugeValueTask<>(
			metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_TP + "." + ATTR_RATE_MEAN),
				new Gauge<Double>() {
					@Override
					public final Double getValue() {
						double x = 0, y = 0;
						try {
							x = mgmtConnExecutor.submit(taskGetBWMean).get();
							y = mgmtConnExecutor.submit(taskGetCountSucc).get();
							x *= y;
							y = mgmtConnExecutor.submit(taskGetCountBytes).get();
						} catch(final InterruptedException | RejectedExecutionException | ExecutionException e) {
							ExceptionHandler.trace(
								LOG, Level.DEBUG, e, "Metric value fetching failed"
							);
						}
						return y==0 ? 0 : x / y;
					}
				}
			)
		);
		taskGetTP1Min = new GaugeValueTask<>(
			metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_TP + "." + ATTR_RATE_1MIN),
				new Gauge<Double>() {
					@Override
					public final Double getValue() {
						double x = 0, y = 0;
						try {
							x = mgmtConnExecutor.submit(taskGetBW1Min).get();
							y = mgmtConnExecutor.submit(taskGetCountSucc).get();
							x *= y;
							y = mgmtConnExecutor.submit(taskGetCountBytes).get();
						} catch(final InterruptedException | RejectedExecutionException | ExecutionException e) {
							ExceptionHandler.trace(
								LOG, Level.DEBUG, e, "Metric value fetching failed"
							);
						}
						return y==0 ? 0 : x / y;
					}
				}
			)
		);
		taskGetTP5Min = new GaugeValueTask<>(
			metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_TP + "." + ATTR_RATE_5MIN),
				new Gauge<Double>() {
					@Override
					public final Double getValue() {
						double x = 0, y = 0;
						try {
							x = mgmtConnExecutor.submit(taskGetBW5Min).get();
							y = mgmtConnExecutor.submit(taskGetCountSucc).get();
							x *= y;
							y = mgmtConnExecutor.submit(taskGetCountBytes).get();
						} catch(final InterruptedException | RejectedExecutionException | ExecutionException e) {
							ExceptionHandler.trace(
								LOG, Level.DEBUG, e, "Metric value fetching failed"
							);
						}
						return y==0 ? 0 : x / y;
					}
				}
			)
		);
		taskGetTP15Min = new GaugeValueTask<>(
			metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_TP + "." + ATTR_RATE_15MIN),
				new Gauge<Double>() {
					@Override
					public final Double getValue() {
						double x = 0, y = 0;
						try {
							x = mgmtConnExecutor.submit(taskGetBW15Min).get();
							y = mgmtConnExecutor.submit(taskGetCountSucc).get();
							x *= y;
							y = mgmtConnExecutor.submit(taskGetCountBytes).get();
						} catch(final InterruptedException | RejectedExecutionException | ExecutionException e) {
							ExceptionHandler.trace(
								LOG, Level.DEBUG, e, "Metric value fetching failed"
							);
						}
						return y==0 ? 0 : x / y;
					}
				}
			)
		);
		taskGetBWMean = new GaugeValueTask<>(metricBWMean);
		taskGetBW1Min = new GaugeValueTask<>(metricBW1Min);
		taskGetBW5Min = new GaugeValueTask<>(metricBW5Min);
		taskGetBW15Min = new GaugeValueTask<>(metricBW15Min);
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
		metaInfoLog = new LogConsumer<>();
		////////////////////////////////////////////////////////////////////////////////////////////
		int
			threadCount = threadCountPerServer * remoteLoadMap.size(),
			queueSize = threadCount * runTimeConfig.getRunRequestQueueFactor();
		submitExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
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
		////////////////////////////////////////////////////////////////////////////////////////////
		threadCount = remoteLoadMap.size() * 20; // metric count is 18
		mgmtConnExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(queueSize),
			new WorkerFactory("getMetricValue")
		) {
			@Override @SuppressWarnings("NullableProblems")
			public final <V> Future<V> submit(final Callable<V> task) {
				Future<V> future = null;
				boolean pass = false;
				int tryCount = 0;
				do {
					try {
						future = super.submit(task);
						pass = true;
					} catch(final RejectedExecutionException e) {
						LOG.trace(Markers.ERR, "JMX task rejected {} times", tryCount);
						tryCount ++;
						try {
							Thread.sleep(retryDelayMilliSec);
						} catch(final InterruptedException ee) {
							LOG.debug(Markers.ERR, "Rejection handling interrupted");
							break;
						}
					}
				} while(!pass && tryCount < retryCountMax);
				if(!pass) {
					LOG.trace(Markers.ERR, "Failed to resubmit the rejected remote management task");
				}
				return future;
			}
		};
		mgmtConnExecutor.prestartAllCoreThreads();
		////////////////////////////////////////////////////////////////////////////////////////////
		metricsReporter.start();
	}
	//
	private Gauge<Long> registerJmxGaugeSum(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(getName(), mBeanName + "." + attrName),
			new Gauge<Long>() {
				//
				private final String fullMBeanName =
					getName().substring(0, getName().lastIndexOf('x')) + '.' + mBeanName;
				//
				@Override
				public final Long getValue() {
					//
					long value = 0;
					MBeanServerConnection nextMBeanConn;
					ObjectName objectName;
					//
					for(final String addr: mBeanSrvConnMap.keySet()) {
						nextMBeanConn = mBeanSrvConnMap.get(addr);
						objectName = null;
						try {
							objectName = new ObjectName(domain, KEY_NAME, fullMBeanName);
						} catch(final MalformedObjectNameException e) {
							LOG.warn(
								Markers.ERR, "Invalid object name \"{}\": {}", mBeanName, e.toString()
							);
						}
						//
						if(objectName != null) {
							try {
								value += (long) nextMBeanConn.getAttribute(objectName, attrName);
							} catch(final AttributeNotFoundException e) {
								LOG.warn(
									Markers.ERR, "Attribute \"{}\" not found for MBean \"{}\" @ {}",
									attrName, objectName.getCanonicalName(), addr
								);
							} catch(final IOException|MBeanException |InstanceNotFoundException |ReflectionException e) {
								ExceptionHandler.trace(
									LOG, Level.DEBUG, e,
									String.format(
										FMT_MSG_FAIL_FETCH_VALUE,
										objectName.getCanonicalName() + "." + attrName, addr
									)
								);
							}
						}
					}
					//
					return value;
				}
			}
		);
	}
	//
	private Gauge<Long> registerJmxGaugeMinLong(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(getName(), mBeanName+"."+attrName),
			new Gauge<Long>() {
				//
				private final String
					fullMBeanName = getName().substring(0, getName().lastIndexOf('x')) + '.' + mBeanName;
				//
				@Override
				public final Long getValue() {
					//
					long value = Long.MAX_VALUE;
					MBeanServerConnection nextMBeanConn;
					ObjectName objectName;
					//
					for(final String addr: mBeanSrvConnMap.keySet()) {
						nextMBeanConn = mBeanSrvConnMap.get(addr);
						objectName = null;
						try {
							objectName = new ObjectName(domain, KEY_NAME, fullMBeanName);
						} catch(final MalformedObjectNameException e) {
							LOG.warn(
								Markers.ERR, "Invalid object name \"{}\": {}",
								mBeanName, e.toString()
							);
						}
						//
						if(objectName!=null) {
							try {
								long t = (long) nextMBeanConn.getAttribute(objectName, attrName);
								if(t < value) {
									value = t;
								}
							} catch(final AttributeNotFoundException e) {
								LOG.warn(
									Markers.ERR, "Attribute \"{}\" not found for MBean \"{}\" @ {}",
									attrName, objectName.getCanonicalName(), addr
								);
							} catch(final IOException|MBeanException|InstanceNotFoundException|ReflectionException e) {
								ExceptionHandler.trace(
									LOG, Level.DEBUG, e,
									String.format(
										FMT_MSG_FAIL_FETCH_VALUE,
										objectName.getCanonicalName() + "." + attrName, addr
									)
								);
							}
						}
					}
					//
					return value==Long.MAX_VALUE ? 0 : value;
				}
			}
		);
	}
	//
	private Gauge<Long> registerJmxGaugeMaxLong(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(getName(), mBeanName+"."+attrName),
			new Gauge<Long>() {
				//
				private final String
					fullMBeanName = getName().substring(0, getName().lastIndexOf('x')) + '.' + mBeanName;
				//
				@Override
				public final Long getValue() {
					//
					long value = Long.MIN_VALUE;
					MBeanServerConnection nextMBeanConn;
					ObjectName objectName;
					//
					for(final String addr: mBeanSrvConnMap.keySet()) {
						nextMBeanConn = mBeanSrvConnMap.get(addr);
						objectName = null;
						try {
							objectName = new ObjectName(domain, KEY_NAME, fullMBeanName);
						} catch(final MalformedObjectNameException e) {
							LOG.warn(
								Markers.ERR, "Invalid object name \"{}\": {}",
								mBeanName, e.toString()
							);
						}
						//
						if(objectName!=null) {
							try {
								long t = (long) nextMBeanConn.getAttribute(objectName, attrName);
								if(t > value) {
									value = t;
								}
							} catch(final AttributeNotFoundException e) {
								LOG.warn(
									Markers.ERR, "Attribute \"{}\" not found for MBean \"{}\" @ {}",
									attrName, objectName.getCanonicalName(), addr
								);
							} catch(final IOException|MBeanException|InstanceNotFoundException|ReflectionException e) {
								ExceptionHandler.trace(
									LOG, Level.DEBUG, e,
									String.format(
										FMT_MSG_FAIL_FETCH_VALUE,
										objectName.getCanonicalName()+"."+attrName, addr
									)
								);
							}
						}
					}
					//
					return value==Long.MIN_VALUE ? 0 : value;
				}
			}
		);
	}
	//
	private Gauge<Double> registerJmxGaugeSumDouble(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(getName(), mBeanName+"."+attrName),
			new Gauge<Double>() {
				//
				private final String
					fullMBeanName = getName().substring(0, getName().lastIndexOf('x')) + '.' + mBeanName;
				//
				@Override
				public final Double getValue() {
					//
					double value = 0;
					MBeanServerConnection nextMBeanConn;
					ObjectName objectName;
					//
					for(final String addr: mBeanSrvConnMap.keySet()) {
						nextMBeanConn = mBeanSrvConnMap.get(addr);
						objectName = null;
						try {
							objectName = new ObjectName(domain, KEY_NAME, fullMBeanName);
						} catch(final MalformedObjectNameException e) {
							LOG.warn(
								Markers.ERR, "Invalid object name \"{}\": {}",
								mBeanName, e.toString()
							);
						}
						//
						if(objectName!=null) {
							try {
								value += (double) nextMBeanConn.getAttribute(objectName, attrName);
							} catch(final AttributeNotFoundException e) {
								LOG.warn(
									Markers.ERR, "Attribute \"{}\" not found for MBean \"{}\" @ {}",
									attrName, objectName.getCanonicalName(), addr
								);
							} catch(final IOException|MBeanException|InstanceNotFoundException|ReflectionException e) {
								ExceptionHandler.trace(
									LOG, Level.DEBUG, e,
									String.format(
										FMT_MSG_FAIL_FETCH_VALUE,
										objectName.getCanonicalName() + "." + attrName, addr
									)
								);
							}
						}
					}
					//
					return value;
				}
			}
		);
	}
	//
	private Gauge<Double> registerJmxGaugeAvgDouble(
		final String domain, final String mBeanName, final String attrName
	) {
		return metrics.register(
			MetricRegistry.name(getName(), mBeanName+"."+attrName),
			new Gauge<Double>() {
				//
				private final String
					fullMBeanName = getName().substring(0, getName().lastIndexOf('x')) + '.' + mBeanName;
				//
				@Override
				public final Double getValue() {
					//
					double value = 0;
					MBeanServerConnection nextMBeanConn;
					ObjectName objectName;
					//
					for(final String addr: mBeanSrvConnMap.keySet()) {
						nextMBeanConn = mBeanSrvConnMap.get(addr);
						objectName = null;
						try {
							objectName = new ObjectName(domain, KEY_NAME, fullMBeanName);
						} catch(final MalformedObjectNameException e) {
							LOG.warn(
								Markers.ERR, "Invalid object name \"{}\": {}",
								mBeanName, e.toString()
							);
						}
						//
						if(objectName!=null) {
							try {
								value += (double) nextMBeanConn.getAttribute(objectName, attrName);
							} catch(final AttributeNotFoundException e) {
								LOG.warn(
									Markers.ERR, "Attribute \"{}\" not found for MBean \"{}\" @ {}",
									attrName, objectName.getCanonicalName(), addr
								);
							} catch(final IOException|MBeanException|InstanceNotFoundException|ReflectionException e) {
								ExceptionHandler.trace(
									LOG, Level.DEBUG, e,
									String.format(
										FMT_MSG_FAIL_FETCH_VALUE,
										objectName.getCanonicalName()+"."+attrName, addr
									)
								);
							}
						}
					}
					//
					return mBeanSrvConnMap.size()==0 ? 0 : value / mBeanSrvConnMap.size();
				}
			}
		);
	}
	//
	private void logMetaInfoFrames() {
		final ArrayList<Future<List<T>>> nextMetaInfoFrameFutures = new ArrayList<>(
			remoteLoadMap.size()
		);
		//
		for(final LoadSvc<T> nextLoadSvc: remoteLoadMap.values()) {
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
		for(final Future<List<T>> nextMetaInfoFrameFuture: nextMetaInfoFrameFutures) {
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
		if(!mgmtConnExecutor.isShutdown() && !mgmtConnExecutor.isTerminated()) {
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
				ExceptionHandler.trace(LOG, Level.WARN, e, "Log remote metrics failed, skipping");
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
					getName(),
					countReqSucc.get(), countReqFail.get(),
					//
					avgLatency.get() / BILLION, (double) minLatency.get() / BILLION,
					medLatency.get() / BILLION, (double) maxLatency.get() / BILLION,
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
					submitExecutor.getQueue().size() + submitExecutor.getActiveCount(),
					countReqFail.get(),
					//
					avgLatency.get() / BILLION, (double) minLatency.get() / BILLION,
					medLatency.get() / BILLION, (double) maxLatency.get() / BILLION,
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
		LoadSvc nextLoadSvc;
		for(final String addr: remoteLoadMap.keySet()) {
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
		super.start();
		LOG.info(Markers.MSG, "Started {}", getName());
	}
	//
	@Override
	public final void run() {
		//
		final int metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
		try {
			if(metricsUpdatePeriodSec > 0) {
				long processedCount = 0;
				while(isAlive()) {
					//
					logMetaInfoFrames();
					logMetrics(Markers.PERF_AVG);
					//
					try {
						processedCount = countRej.get() + countReqSucc.get() + countReqFail.get();
					} catch(final ExecutionException e) {
						ExceptionHandler.trace(LOG, Level.DEBUG, e, "Failed to get metric value");
					}
					//
					if(maxCount <= processedCount) {
						break;
					} else if(lock.tryLock()) {
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
			} else {
				final Thread waitThread = new Thread(
					new CountLimitWaitTask(
						maxCount, mgmtConnExecutor,
						new GaugeValueTask[] {taskGetCountSucc, taskGetCountFail, taskGetCountRej}
					)
				);
				waitThread.start();
				waitThread.join();
			}
			LOG.debug(Markers.MSG, "Finish reached");
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		} finally {
			interrupt();
		}
	}
	//
	@Override
	public final void interrupt() {
		LOG.debug(Markers.MSG, "Interrupting {}...", getName());
		final int reqTimeOutMilliSec = runTimeConfig.getRunReqTimeOutMilliSec();
		//
		if(!submitExecutor.isShutdown()) {
			submitExecutor.shutdown();
		}
		//
		if(!submitExecutor.isTerminated()) {
			try {
				submitExecutor.awaitTermination(reqTimeOutMilliSec, TimeUnit.MILLISECONDS);
			} catch(final InterruptedException e) {
				LOG.debug(Markers.ERR, "Interrupted waiting for submit executor to finish");
			}
		}
		//
		final ExecutorService interruptExecutor = Executors.newFixedThreadPool(remoteLoadMap.size());
		//
		for(final String addr: remoteLoadMap.keySet()) {
			interruptExecutor.submit(
				new Runnable() {
					@Override
					public final void run() {
						try {
							remoteLoadMap.get(addr).interrupt();
							LOG.trace(Markers.MSG, "Interrupted remote service @ {}", addr);
						} catch(final IOException e) {
							ExceptionHandler.trace(
								LOG, Level.DEBUG, e,
								String.format("Failed to interrupt remote load service @ %s", addr)
							);
						}
					}
				}
			);
		}
		//
		interruptExecutor.shutdown();
		try {
			interruptExecutor.awaitTermination(reqTimeOutMilliSec, TimeUnit.MILLISECONDS);
		} catch(final InterruptedException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupting was interrupted");
		}
		//
		super.interrupt();
		LOG.debug(Markers.MSG, "Interrupted {}", getName());
	}
	//
	@Override
	public final void close()
	throws IOException {
		synchronized(remoteLoadMap) {
			if(!remoteLoadMap.isEmpty()) {
				if(!isInterrupted()) {
					interrupt();
				}
				//
				LOG.debug(Markers.MSG, "log summary metrics");
				logMetrics(Markers.PERF_SUM);
				LOG.debug(Markers.MSG, "log metainfo frames");
				logMetaInfoFrames();
				//
				LoadSvc<T> nextLoadSvc;
				JMXConnector nextJMXConn = null;
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void submit(final T dataItem) {
		if(maxCount > submitExecutor.getTaskCount()) {
			if(dataItem == null) {
				LOG.debug(Markers.MSG, "{}: poison submitted");
				// determine the max count now
				maxCount = submitExecutor.getTaskCount();
				//
				for(final String addr: remoteLoadMap.keySet()) {
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
				submitExecutor.shutdown();
			} else {
				final Object addrs[] = remoteLoadMap.keySet().toArray();
				final String addr = String.class.cast(
					addrs[(int) submitExecutor.getTaskCount() % addrs.length]
				);
				final RemoteSubmitTask<T> remoteSubmitTask = RemoteSubmitTask
					.getInstanceFor(remoteLoadMap.get(addr), dataItem);
				int rejectCount = 0;
				do {
					try {
						submitExecutor.submit(remoteSubmitTask);
						break;
					} catch(final RejectedExecutionException e) {
						rejectCount ++;
						try {
							Thread.sleep(rejectCount * retryDelayMilliSec);
						} catch(final InterruptedException ee) {
							break;
						}
					}
				} while(rejectCount < retryCountMax && !submitExecutor.isShutdown());
			}
		} else {
			LOG.debug(
				Markers.MSG,
				"{}: max data item count ({}) have been submitted, shutdown the submit executor",
				getName(), maxCount
			);
			submitExecutor.shutdown();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
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
	@Override @SuppressWarnings("unchecked")
	public final void setConsumer(final Consumer<T> consumer)
	throws RemoteException {
		if(LoadClient.class.isInstance(consumer)) {
			// consumer is client which has the map of consumers
			try {
				final LoadClient<T> loadClient = (LoadClient<T>) consumer;
				final Map<String, LoadSvc<T>> consumeMap = loadClient.getRemoteLoadMap();
				LOG.debug(Markers.MSG, "Consumer is LoadClient instance");
				for(final String addr: consumeMap.keySet()) {
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
				for(final String addr: remoteLoadMap.keySet()) {
					remoteLoadMap.get(addr).setConsumer(loadSvc);
				}
			} catch(final ClassCastException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Data item class mismatch");
			}
		} else if(DataItemBufferClient.class.isInstance(consumer)) {
			try {
				final DataItemBufferClient<T> mediator = (DataItemBufferClient<T>) consumer;
				LOG.debug(Markers.MSG, "Consumer is remote mediator buffer");
				for(final String addr: remoteLoadMap.keySet()) {
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
	//
	@Override
	public final Consumer<T> getConsumer() {
		return null;
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
	public final String toString() {
		return getName();
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
		final Object addrs[] = remoteLoadMap.keySet().toArray();
		final String addr = String.class.cast(
			addrs[(int) submitExecutor.getTaskCount() % addrs.length]
		);
		remoteLoadMap.get(addr).handleResult(task, result);
	}
	//
	private volatile int i = 0;
	@Override
	public String getNextNodeAddr() {
		final String storageAddrs[] = runTimeConfig.getStorageAddrs();
		return storageAddrs[i ++ % storageAddrs.length];
	}
	//
	@Override
	public final Future<AsyncIOTask.Result> submit(final AsyncIOTask<T> request)
	throws RemoteException {
		final Object addrs[] = remoteLoadMap.keySet().toArray();
		final String addr = String.class.cast(
			addrs[(int) submitExecutor.getTaskCount() % addrs.length]
		);
		return remoteLoadMap.get(addr).submit(request);
	}
}
