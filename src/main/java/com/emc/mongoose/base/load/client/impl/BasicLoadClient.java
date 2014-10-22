package com.emc.mongoose.base.load.client.impl;
//
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.persist.LogConsumer;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.base.load.impl.SubmitDataItemTask;
import com.emc.mongoose.base.load.client.LoadClient;
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 20.10.14.
 */
public class BasicLoadClient<T extends DataItem>
extends Thread
implements LoadClient<T> {
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Map<String, LoadSvc<T>> remoteLoadMap;
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
	private final static class GetGaugeValue<V extends Number>
		implements Callable<V> {
		//
		private final Gauge<V> gauge;
		//
		public GetGaugeValue(final Gauge<V> gauge) {
			this.gauge = gauge;
		}
		//
		@Override
		public final V call()
			throws Exception {
			return gauge.getValue();
		}
	}
	@SuppressWarnings("FieldCanBeLocal")
	private final Gauge<Long>
		metricSuccCount, metricByteCount;
	@SuppressWarnings("FieldCanBeLocal")
	private final Gauge<Double>
		metricBWMean, metricBW1Min, metricBW5Min, metricBW15Min;
	@SuppressWarnings("FieldCanBeLocal")
	private final GetGaugeValue<Long>
		countSubmGetter, countRejGetter, countSuccGetter, countFailGetter,
		minDurGetter, maxDurGetter, countBytesGetter, countNanoSecGetter;
	private final GetGaugeValue<Double>
		meanTPGetter, oneMinTPGetter, fiveMinTPGetter, fifteenMinTPGetter, meanBWGetter,
		oneMinBWGetter, fiveMinBWGetter, fifteenMinBWGetter, medDurGetter, avgDurGetter;
	////////////////////////////////////////////////////////////////////////////////////////////////
	private volatile long maxCount;
	//
	private final ThreadPoolExecutor submitExecutor, mgmtConnExecutor;
	private final LogConsumer<T> metaInfoLog;
	private final RunTimeConfig runTimeConfig;
	private final int retryCountMax, retryDelayMilliSec;
	@SuppressWarnings("FieldCanBeLocal")
	private final RequestConfig<T> reqConf;
	//
	public BasicLoadClient(
		final RunTimeConfig runTimeConfig,
		final Map<String, LoadSvc<T>> remoteLoadMap,
		final Map<String, JMXConnector> remoteJMXConnMap,
		final RequestConfig<T> reqConf,
		final long maxCount, final int threadCountPerServer
	) {
		//
		this.runTimeConfig = runTimeConfig;
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		final MBeanServer mBeanServer = ServiceUtils.getMBeanServer(
			runTimeConfig.getRemoteMonitorPort()
		);
		metricsReporter = JmxReporter.forRegistry(metrics)
			.convertDurationsTo(TimeUnit.SECONDS)
			.convertRatesTo(TimeUnit.SECONDS)
			.registerWith(mBeanServer)
			.build();
		////////////////////////////////////////////////////////////////////////////////////////////
		this.remoteLoadMap = remoteLoadMap;
		this.remoteJMXConnMap = remoteJMXConnMap;
		this.reqConf = reqConf;
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
		int
			threadCount = threadCountPerServer*remoteLoadMap.size(),
			queueSize = threadCount * runTimeConfig.getRunRequestQueueFactor();
		submitExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(queueSize)
		);
		//
		threadCount = remoteLoadMap.size()*20; // metric count is 18
		mgmtConnExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(queueSize)
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
					mgmtConnExecutor.submit(
						new Callable<List<T>>() {
							@Override
							public final List<T> call()
								throws Exception {
								return nextLoadSvc.takeFrame();
							}
						}
					)
				);
			} catch(final RejectedExecutionException e) {
				ExceptionHandler.trace(LOG, Level.DEBUG, e, "");
			}
		}
		//
		List<T> nextMetaInfoFrame = null;
		for(final Future<List<T>> nextMetaInfoFrameFuture: nextMetaInfoFrameFutures) {
			//
			try {
				nextMetaInfoFrame = nextMetaInfoFrameFuture.get();
			} catch(final ExecutionException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to fetch the metainfo frame");
			} catch(final InterruptedException e) {
				try {
					nextMetaInfoFrame = nextMetaInfoFrameFuture.get();
				} catch(final InterruptedException|ExecutionException ee) {
					ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to fetch the metainfo frame");
				}
			}
			//
			if(nextMetaInfoFrame!=null && nextMetaInfoFrame.size()>0) {
				for(final T nextMetaInfoRec: nextMetaInfoFrame) {
					metaInfoLog.submit(nextMetaInfoRec);
				}
			}
			//
		}
	}
	//
	Future<Long>
		countSubm, countRej, countReqSucc, countReqFail,
	/*countNanoSec, countBytes, */minDur, maxDur;
	Future<Double>
		meanTP, oneMinTP, fiveMinTP, fifteenMinTP,
		meanBW, oneMinBW, fiveMinBW, fifteenMinBW,
		medDur, avgDur;
	//
	private void logMetrics(final Marker logMarker) {
		//
		try {
			countSubm = mgmtConnExecutor.submit(countSubmGetter);
			countRej = mgmtConnExecutor.submit(countRejGetter);
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
		} catch(final RejectedExecutionException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Log remote metrics failed, skipping");
		}
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
		LoadSvc nextLoadSvc;
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
		// register shutdown hook which should perform correct server-side shutdown even if
		// user hits ^C
		Runtime.getRuntime().addShutdownHook(
			new Thread() {
				@Override
				public final void run() {
					LOG.info(Markers.MSG, "Shutdown hook start");
					try {
						close();
						LOG.debug(Markers.MSG, "Shutdown hook finished successfully");
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
		final int metricsUpdatePeriodSec = runTimeConfig.getRunMetricsPeriodSec();
		//
		try {
			do {
				try {
					logMetrics(Markers.PERF_AVG);
					logMetaInfoFrames();
					TimeUnit.SECONDS.sleep(metricsUpdatePeriodSec); // sleep
					countDone = countReqSucc.get() + countReqFail.get() + countRej.get();
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
		final int reqTimeOutMilliSec = runTimeConfig.getRunReqTimeOutMilliSec();
		final LinkedList<Thread> svcInterruptThreads = new LinkedList<>();
		svcInterruptThreads.add(
			new Thread("interrupt-submit-" + getName()) {
				@Override
				public final void run() {
					submitExecutor.shutdown();
					while(
						submitExecutor.getQueue().size() > 0
							||
						submitExecutor.getCorePoolSize() == submitExecutor.getActiveCount()
					) {
						try {
							Thread.sleep(retryDelayMilliSec);
						} catch(final InterruptedException e) {
							break;
						}
					}
					final int droppedTaskCount = submitExecutor.shutdownNow().size();
					if(droppedTaskCount > 0) {
						LOG.info(Markers.ERR, "Dropped {} tasks", droppedTaskCount);
					}
				}
			}
		);
		svcInterruptThreads.getLast().start();
		try {
			svcInterruptThreads.getLast().join();
			svcInterruptThreads.removeLast();
		} catch(final InterruptedException e) {
			ExceptionHandler.trace(
				LOG, Level.DEBUG, e, "Interrupted while interrupting the submitter"
			);
		}
		//
		for(final String addr: remoteLoadMap.keySet()) {
			svcInterruptThreads.add(
				new Thread("interrupt-svc-" + getName() + "-" + addr) {
					@Override
					public final void run() {
						try {
							remoteLoadMap.get(addr).interrupt();
							LOG.trace(Markers.MSG, "Interrupted remote service @ {}", addr);
						} catch(final IOException e) {
							ExceptionHandler.trace(
								LOG, Level.WARN, e,
								"Failed to interrupt remote load service @ " + addr
							);
						}
					}
				}
			);
			svcInterruptThreads.getLast().start();
		}
		//
		for(final Thread svcInterruptThread: svcInterruptThreads) {
			try {
				svcInterruptThread.join(reqTimeOutMilliSec);
				LOG.debug(Markers.MSG, "Finished: \"{}\"", svcInterruptThread);
			} catch(final InterruptedException e) {
				ExceptionHandler.trace(LOG, Level.DEBUG, e, "Interrupted");
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
		//
		LoadSvc<T> nextLoadSvc;
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
		mgmtConnExecutor.shutdownNow();
		metricsReporter.close();
		//
		LOG.debug(Markers.MSG, "Closing the remote services...");
		for(final String addr: remoteLoadMap.keySet()) {
			//
			try {
				nextLoadSvc = remoteLoadMap.get(addr);
				LOG.debug(Markers.MSG, "Closing server instance @ {}...", addr);
				nextLoadSvc.close();
				LOG.info(Markers.MSG, "Server instance @ {} has been closed", addr);
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
		LOG.debug(Markers.MSG, "Clear the servers map");
		remoteLoadMap.clear();
		LOG.debug(Markers.MSG, "Closed {}", getName());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void submit(final T dataItem) {
		if(maxCount > submitExecutor.getTaskCount()) {
			if(dataItem==null) { // poison
				LOG.trace(
					Markers.MSG, "Got poison on #{}, invoking the soft interruption",
					submitExecutor.getTaskCount()
				);
				maxCount = submitExecutor.getTaskCount();
			} else {
				final Object addrs[] = remoteLoadMap.keySet().toArray();
				final String addr = String.class.cast(
					addrs[(int) submitExecutor.getTaskCount() % addrs.length]
				);
				final SubmitDataItemTask<T, LoadSvc<T>> submTask = new SubmitDataItemTask<>(
					dataItem, remoteLoadMap.get(addr)
				);
				boolean passed = false;
				int rejectCount = 0;
				do {
					try {
						submitExecutor.submit(submTask);
						passed = true;
					} catch(final RejectedExecutionException e) {
						rejectCount ++;
						try {
							Thread.sleep(rejectCount * retryDelayMilliSec);
						} catch(final InterruptedException ee) {
							break;
						}
					}
				} while(!passed && rejectCount < retryCountMax);
			}
		} else {
			LOG.debug(Markers.MSG, "All {} tasks submitted", maxCount);
			maxCount = submitExecutor.getTaskCount();
			Thread.currentThread().interrupt(); // interrupt the producer
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
	public final void setConsumer(final Consumer<T> load)
		throws RemoteException {
		try { // consumer is map of consumers
			final LoadClient<T> loadClient = (LoadClient<T>) load;
			final Map<String, LoadSvc<T>> consumeMap = loadClient.getRemoteLoadMap();
			LOG.debug(Markers.MSG, "Consumer is LoadClient instance");
			for(final String addr: consumeMap.keySet()) {
				remoteLoadMap.get(addr).setConsumer(consumeMap.get(addr));
			}
		} catch(final ClassCastException e) {
			try { // single consumer for all these producers
				final LoadSvc loadSvc = LoadSvc.class.cast(load);
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
}
