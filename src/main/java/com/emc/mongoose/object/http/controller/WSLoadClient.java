package com.emc.mongoose.object.http.controller;
//
import com.emc.mongoose.Producer;
import com.emc.mongoose.api.RequestConfig;
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.persist.LogConsumer;
import com.emc.mongoose.logging.ExceptionHandler;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.Consumer;
import com.emc.mongoose.LoadExecutor;
import com.emc.mongoose.object.http.data.WSObject;
import com.emc.mongoose.remote.LoadService;
//
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.remote.ServiceUtils;
import com.emc.mongoose.threading.RejectedTaskHandler;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 08.05.14.
 */
public class WSLoadClient
extends Thread
implements LoadExecutor<WSObject> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Map<String, LoadService<WSObject>> remoteLoadMap;
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final Map<String, JMXConnector> remoteJMXConnMap;
	private final Map<String, MBeanServerConnection> mBeanSrvConnMap;
	private final MetricRegistry metrics = new MetricRegistry();
	private final static MBeanServer MBEAN_SERVER = ServiceUtils.getMBeanServer(
		RunTimeConfig.getInt("remote.monitor.port")
	);
	private final static String DEFAULT_DOMAIN = "metrics";
	protected final JmxReporter metricsReporter = JmxReporter.forRegistry(metrics)
		.convertDurationsTo(TimeUnit.SECONDS)
		.convertRatesTo(TimeUnit.SECONDS)
		.registerWith(MBEAN_SERVER)
		.build();
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
	private final static class GetGaugeValue<T extends Number>
	implements Callable<T> {
		//
		private final Gauge<T> gauge;
		//
		public GetGaugeValue(final Gauge<T> gauge) {
			this.gauge = gauge;
		}
		//
		@Override
		public final T call()
		throws Exception {
			return gauge.getValue();
		}
	}
	private final Gauge<Long>
		metricSuccCount, metricByteCount;
	private final Gauge<Double>
		metricBWMean, metricBW1Min, metricBW5Min, metricBW15Min;
	private final GetGaugeValue<Long>
		countSubmGetter, countRejGetter, countSuccGetter, countFailGetter,
		minDurGetter, maxDurGetter, countBytesGetter, countNanoSecGetter;
	private final GetGaugeValue<Double>
		meanTPGetter, oneMinTPGetter, fiveMinTPGetter, fifteenMinTPGetter, meanBWGetter,
		oneMinBWGetter, fiveMinBWGetter, fifteenMinBWGetter, medDurGetter, avgDurGetter;
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final AtomicLong localSubmCount = new AtomicLong(0);
	private long maxCount;
	//
	private final ThreadPoolExecutor submitExecutor, mgmtConnExecutor;
	private final LogConsumer<WSObject> metaInfoLog;
	//
	public WSLoadClient(
		final Map<String, LoadService<WSObject>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap,
		final long maxCount, final int driverThreadCount
	) {
		////////////////////////////////////////////////////////////////////////////////////////////
		this.remoteLoadMap = remoteLoadMap;
		this.remoteJMXConnMap = remoteJMXConnMap;
		//
		try {
			final Object remoteLoads[] = remoteLoadMap.values().toArray();
			this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
			setName(LoadService.class.cast(remoteLoads[0]).getName()+'x'+remoteLoads.length);
		} catch(final NoSuchElementException|NullPointerException e) {
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
		countSubmGetter = new GetGaugeValue<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_SUBM, ATTR_COUNT)
		);
		countRejGetter = new GetGaugeValue<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_REJ, ATTR_COUNT)
		);
		countSuccGetter = new GetGaugeValue<>(metricSuccCount);
		countFailGetter = new GetGaugeValue<>(
			registerJmxGaugeSum(DEFAULT_DOMAIN, METRIC_NAME_FAIL, ATTR_COUNT)
		);
		countNanoSecGetter = new GetGaugeValue<>(
			registerJmxGaugeSum(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_COUNT
			)
		);
		countBytesGetter = new GetGaugeValue<>(metricByteCount);
		minDurGetter = new GetGaugeValue<>(
			registerJmxGaugeMinLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MIN
			)
		);
		maxDurGetter = new GetGaugeValue<>(
			registerJmxGaugeMaxLong(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MAX
			)
		);
		meanTPGetter = new GetGaugeValue<>(
			metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_TP + "." + ATTR_RATE_MEAN),
				new Gauge<Double>() {
					@Override
					public final Double getValue() {
						double x = 0, y = 0;
						try {
							x = mgmtConnExecutor.submit(meanBWGetter).get();
							y = mgmtConnExecutor.submit(countSuccGetter).get();
							x *= y;
							y = mgmtConnExecutor.submit(countBytesGetter).get();
						} catch(final ExecutionException e) {
							ExceptionHandler.trace(LOG, Level.DEBUG, e, "Metric value fetching failed");
						} catch(final InterruptedException e) {
							LOG.debug(Markers.ERR, "Interrupted during metric value fetching");
						}
						return y==0 ? 0 : x / y;
					}
				}
			)
		);
		oneMinTPGetter = new GetGaugeValue<>(
			metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_TP + "." + ATTR_RATE_1MIN),
				new Gauge<Double>() {
					@Override
					public final Double getValue() {
						double x = 0, y = 0;
						try {
							x = mgmtConnExecutor.submit(oneMinBWGetter).get();
							y = mgmtConnExecutor.submit(countSuccGetter).get();
							x *= y;
							y = mgmtConnExecutor.submit(countBytesGetter).get();
						} catch(final ExecutionException e) {
							ExceptionHandler.trace(LOG, Level.DEBUG, e, "Metric value fetching failed");
						} catch(final InterruptedException e) {
							LOG.debug(Markers.ERR, "Interrupted during metric value fetching");
						}
						return y==0 ? 0 : x / y;
					}
				}
			)
		);
		fiveMinTPGetter = new GetGaugeValue<>(
			metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_TP + "." + ATTR_RATE_5MIN),
				new Gauge<Double>() {
					@Override
					public final Double getValue() {
						double x = 0, y = 0;
						try {
							x = mgmtConnExecutor.submit(fiveMinBWGetter).get();
							y = mgmtConnExecutor.submit(countSuccGetter).get();
							x *= y;
							y = mgmtConnExecutor.submit(countBytesGetter).get();
						} catch(final ExecutionException e) {
							ExceptionHandler.trace(LOG, Level.DEBUG, e, "Metric value fetching failed");
						} catch(final InterruptedException e) {
							LOG.debug(Markers.ERR, "Interrupted during metric value fetching");
						}
						return y==0 ? 0 : x / y;
					}
				}
			)
		);
		fifteenMinTPGetter = new GetGaugeValue<>(
			metrics.register(
				MetricRegistry.name(getName(), METRIC_NAME_TP + "." + ATTR_RATE_15MIN),
				new Gauge<Double>() {
					@Override
					public final Double getValue() {
						double x = 0, y = 0;
						try {
							x = mgmtConnExecutor.submit(fifteenMinBWGetter).get();
							y = mgmtConnExecutor.submit(countSuccGetter).get();
							x *= y;
							y = mgmtConnExecutor.submit(countBytesGetter).get();
						} catch(final ExecutionException e) {
							ExceptionHandler.trace(LOG, Level.DEBUG, e, "Metric value fetching failed");
						} catch(final InterruptedException e) {
							LOG.debug(Markers.ERR, "Interrupted during metric value fetching");
						}
						return y==0 ? 0 : x / y;
					}
				}
			)
		);
		meanBWGetter = new GetGaugeValue<>(metricBWMean);
		oneMinBWGetter = new GetGaugeValue<>(metricBW1Min);
		fiveMinBWGetter = new GetGaugeValue<>(metricBW5Min);
		fifteenMinBWGetter = new GetGaugeValue<>(metricBW15Min);
		medDurGetter = new GetGaugeValue<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_MED
			)
		);
		avgDurGetter = new GetGaugeValue<>(
			registerJmxGaugeAvgDouble(
				DEFAULT_DOMAIN, METRIC_NAME_REQ + "." + METRIC_NAME_DUR, ATTR_AVG
			)
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		metaInfoLog = new LogConsumer<>();
		////////////////////////////////////////////////////////////////////////////////////////////
		int threadCount = driverThreadCount*remoteLoadMap.size();
		submitExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(threadCount*REQ_QUEUE_FACTOR),
			new RejectedTaskHandler()
		);
		//
		threadCount = remoteLoadMap.size()*20; // metric count is 18
		mgmtConnExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(threadCount*REQ_QUEUE_FACTOR),
			new RejectedTaskHandler()
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		metricsReporter.start();
	}
	//
	private Gauge<Long> registerJmxGaugeSum(
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
						if(objectName!=null) {
							try {
								value += (long) nextMBeanConn.getAttribute(objectName, attrName);
							} catch(final AttributeNotFoundException e) {
								LOG.warn(
									Markers.ERR, "Attribute \"{}\" not found for MBean \"{}\" @ {}",
									attrName, objectName.getCanonicalName(), addr
								);
							} catch(final IOException|MBeanException|InstanceNotFoundException|ReflectionException e) {
								ExceptionHandler.trace(
									LOG, Level.WARN, e,
									String.format(
										"Value fetching failed for metric \"%s\" from %s",
										objectName.getCanonicalName()+"."+attrName, addr
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
									LOG, Level.WARN, e,
									String.format(
										"Value fetching failed for metric \"%s\" from %s",
										objectName.getCanonicalName()+"."+attrName, addr
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
									LOG, Level.WARN, e,
									String.format(
										"Value fetching failed for metric \"%s\" from %s",
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
									LOG, Level.WARN, e,
									String.format(
										"Value fetching failed for metric \"%s\" from %s",
										objectName.getCanonicalName()+"."+attrName, addr
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
									LOG, Level.WARN, e,
									String.format(
										"Failed to fetch the value for \"{}\" from {}",
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
		final ArrayList<Future<List<WSObject>>> nextMetaInfoFrameFutures = new ArrayList<>(
			remoteLoadMap.size()
		);
		//
		for(final LoadService<WSObject> nextLoadSvc: remoteLoadMap.values()) {
			nextMetaInfoFrameFutures.add(
				mgmtConnExecutor.submit(
					new Callable<List<WSObject>>() {
						@Override
						public final
						List<WSObject> call()
							throws Exception {
							return nextLoadSvc.takeFrame();
						}
					}
				)
			);
		}
		//
		List<WSObject> nextMetaInfoFrame = null;
		for(final Future<List<WSObject>> nextMetaInfoFrameFuture: nextMetaInfoFrameFutures) {
			//
			try {
				nextMetaInfoFrame = nextMetaInfoFrameFuture.get();
			} catch(final ExecutionException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to fetch the metainfo frame");
			} catch(final InterruptedException e) {
				LOG.debug(Markers.ERR, "Interrupted while fetching the metainfo frame");
			}
			//
			if(nextMetaInfoFrame!=null && nextMetaInfoFrame.size()>0) {
				for(final WSObject nextMetaInfoRec: nextMetaInfoFrame) {
					metaInfoLog.submit(nextMetaInfoRec);
				}
			}
			//
		}
	}
	//
	Future<Long>
		countSubm, /*countRej, */countReqSucc, countReqFail,
		/*countNanoSec, countBytes, */minDur, maxDur;
	Future<Double>
		meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
		meanBW, oneMinBW, fiveMinBW, fifteenMinBW,
		medDur, avgDur;
	//
	private void logMetrics(final Marker logMarker) {
		//
		countSubm = mgmtConnExecutor.submit(countSubmGetter);
		//countRej = mgmtConnExecutor.submit(countRejGetter);
		countReqSucc = mgmtConnExecutor.submit(countSuccGetter);
		countReqFail = mgmtConnExecutor.submit(countFailGetter);
		//countNanoSec = mgmtConnExecutor.submit(countNanoSecGetter);
		//countBytes = mgmtConnExecutor.submit(countBytesGetter);
		minDur = mgmtConnExecutor.submit(minDurGetter);
		maxDur = mgmtConnExecutor.submit(maxDurGetter);
		meanTP = mgmtConnExecutor.submit(meanTPGetter);
		oneMinTP = mgmtConnExecutor.submit(oneMinTPGetter);
		fiveMinTP = mgmtConnExecutor.submit(fiveMinTPGetter);
		fifteenMinTP = mgmtConnExecutor.submit(fifteenMinTPGetter);
		meanBW = mgmtConnExecutor.submit(meanBWGetter);
		oneMinBW = mgmtConnExecutor.submit(oneMinBWGetter);
		fiveMinBW = mgmtConnExecutor.submit(fiveMinBWGetter);
		fifteenMinBW = mgmtConnExecutor.submit(fifteenMinBWGetter);
		medDur = mgmtConnExecutor.submit(medDurGetter);
		avgDur = mgmtConnExecutor.submit(avgDurGetter);
		//
		//
		try {
			LOG.info(
				logMarker,
				MSG_FMT_METRICS.format(
					new Object[] {
						//
						countReqSucc.get(),
						submitExecutor.getQueue().size() + submitExecutor.getActiveCount(),
						countReqFail.get(),
						//
						(double) minDur.get() / BILLION,
						medDur.get() / BILLION,
						avgDur.get() / BILLION,
						(double) maxDur.get() / BILLION,
						//
						meanTP.get(), oneMinTP.get(), fiveMinTP.get(), fifteenMinTP.get(),
						//
						meanBW.get()/MIB,
						oneMinBW.get()/MIB, fiveMinBW.get()/MIB, fifteenMinBW.get()/MIB
					}
				)
			);
		} catch(final ExecutionException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to fetch the metrics");
		} catch(final InterruptedException e) {
			LOG.debug(Markers.ERR, "Interrupted while fetching the metric");
		}
		//
	}
	//
	@Override
	public final void start() {
		LoadService nextLoadSvc;
		for(final String addr: remoteLoadMap.keySet()) {
			nextLoadSvc = remoteLoadMap.get(addr);
			try {
				nextLoadSvc.start();
				LOG.info(
					Markers.MSG, "{} started bound to remote service @{}",
					nextLoadSvc.getName(), addr
				);
			} catch(final IOException e) {
				LOG.error(Markers.ERR, "Failed to start remote load @" + addr, e);
			}
		}
		// register shutdown hook which should perform correct driver-side shutdown even if
		// user hits ^C
		Runtime.getRuntime().addShutdownHook(
			new Thread() {
				@Override
				public final void run() {
					try {
						close();
					} catch(final IOException e) {
						e.printStackTrace();
					}
				}
			}
		);
		LOG.trace(Markers.MSG, "Registered shutdown hook");
		//
		super.start();
	}
	//
	@Override
	public final void run() {
		//
		long countDone;
		//
		try {
			do {
				try {
					logMetrics(Markers.PERF_AVG);
					logMetaInfoFrames();
					TimeUnit.SECONDS.sleep(METRICS_UPDATE_PERIOD_SEC); // sleep
					countDone = countReqSucc.get() + countReqFail.get();
				} catch(final InterruptedException e) {
					break;
				}
			} while(countDone < maxCount);
		} catch(final ExecutionException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Failure");
		}
		//
		interrupt();
		LOG.debug(Markers.MSG, "Exiting the monitor thread");
	}
	//
	@Override
	public final void interrupt() {
		LOG.debug(Markers.MSG, "Interrupting {}...", getName());
		//
		submitExecutor.shutdown();
		submitExecutor.getQueue().clear();
		try {
			submitExecutor.awaitTermination(
				RequestConfig.REQUEST_TIMEOUT_MILLISEC, TimeUnit.MILLISECONDS
			);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.ERR, "Awaiting the submit executor termination has been interrupted");
		}
		//
		LinkedList<Thread> svcInterruptThreads = new LinkedList<>();
		for(final String addr: remoteLoadMap.keySet()) {
			svcInterruptThreads.add(
				new Thread("svcInterrupter@"+addr) {
					@Override
					public final void run() {
						try {
							remoteLoadMap.get(addr).interrupt();
							LOG.trace(Markers.MSG, "Interrupted remote service @ {}", addr);
						} catch(final IOException e) {
							LOG.warn(Markers.ERR, "Failed to interrupt remote load service @ {}", addr);
							LOG.debug(Markers.ERR, e.toString(), e.getCause());
						}
					}
				}
			);
			svcInterruptThreads.getLast().start();
		}
		//
		for(final Thread svcInterruptThread: svcInterruptThreads) {
			try {
				svcInterruptThread.join();
			} catch(final InterruptedException e) {
				LOG.debug(Markers.ERR, e.toString(), e.getCause());
			}
		}
		//
		super.interrupt();
		LOG.debug(Markers.MSG, "Interrupted {}", getName());
	}
	//
	@Override
	public final synchronized void close()
		throws IOException {
		//
		if(!isInterrupted()) {
			interrupt();
		}
		LOG.debug(Markers.MSG, "Controller dropped {} tasks", submitExecutor.shutdownNow().size());
		//
		LoadService<WSObject> nextLoadSvc;
		JMXConnector nextJMXConn = null;
		//
		synchronized(LOG) {
			if(!remoteLoadMap.isEmpty()) { // if have not been closed before
				LOG.info(Markers.PERF_SUM, "Summary metrics below for {}", getName());
				logMetaInfoFrames();
				logMetrics(Markers.PERF_SUM);
			}
		}
		//
		LOG.debug(Markers.MSG, "Closing the drivers...");
		for(final String addr: remoteLoadMap.keySet()) {
			//
			try {
				nextLoadSvc = remoteLoadMap.get(addr);
				LOG.debug(Markers.MSG, "Closing driver instance @ {}...", addr);
				nextLoadSvc.close();
				LOG.info(Markers.MSG, "Driver instance @ {} has been closed", addr);
			} catch(final NoSuchElementException e) {
				LOG.debug(Markers.ERR, "Looks like the remote load service is already shut down");
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
				LOG.warn(Markers.ERR, "Failed to close remote load JMX connection "+nextJMXConn);
				LOG.trace(Markers.ERR, e.toString(), e.getCause());
			}
			//
		}
		mgmtConnExecutor.shutdownNow();
		metricsReporter.close();
		//
		LOG.debug(Markers.MSG, "Clear the drivers map");
		remoteLoadMap.clear();
		LOG.debug(Markers.MSG, "Closed {}", getName());
	}
	//
	@Override
	public final void submit(final WSObject data) {
		submitExecutor.submit(
			new Runnable() {
				@Override
				public final
				void run() {
					try {
						if(maxCount > localSubmCount.get()) {
							if(data==null) { // poison
								LOG.trace(Markers.MSG, "Got poison, invoking the interruption");
								maxCount = localSubmCount.get();
							} else {
								final Object addrs[] = remoteLoadMap.keySet().toArray();
								final String addr = String.class.cast(
									addrs[(int)localSubmCount.get()%addrs.length]
								);
								remoteLoadMap.get(addr).submit(data);
								localSubmCount.incrementAndGet();
							}
						} else {
							LOG.info(Markers.MSG, "All {} tasks submitted", maxCount);
							maxCount = localSubmCount.get();
							Thread.currentThread().interrupt(); // interrupt the producer
						}
					} catch(final RemoteException e) {
						LOG.warn(Markers.ERR, "Failure", e);
					}
				}
			}
		);
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
	@Override @SuppressWarnings("unchecked")
	public final void setConsumer(final Consumer<WSObject> load)
		throws RemoteException {
		try { // consumer is map of consumers
			final WSLoadClient loadClient = WSLoadClient.class.cast(load);
			final Map<String, LoadService<WSObject>> consumeMap = loadClient.remoteLoadMap;
			LOG.debug(Markers.MSG, "Consumer is LoadClient instance");
			for(final String addr: consumeMap.keySet()) {
				remoteLoadMap.get(addr).setConsumer(consumeMap.get(addr));
			}
		} catch(final ClassCastException e) {
			try { // single consumer for all these producers
				final LoadService loadSvc = LoadService.class.cast(load);
				LOG.debug(Markers.MSG, "Consumer is RemoteLoad instance");
				for(final String addr: remoteLoadMap.keySet()) {
					remoteLoadMap.get(addr).setConsumer(loadSvc);
				}
			} catch(final ClassCastException ee) {
				LOG.error(
					Markers.ERR, "Unsupported consumer type: {}",
					load.getClass().getCanonicalName()
				);
			}
		}
	}
	//
	@Override
	public final Consumer<WSObject> getConsumer() {
		return null;
	}
	//
	@Override
	public final Producer<WSObject> getProducer()
	throws RemoteException {
		return remoteLoadMap.entrySet().iterator().next().getValue().getProducer();
	}
	//
}
