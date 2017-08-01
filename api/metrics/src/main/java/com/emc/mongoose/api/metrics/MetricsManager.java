package com.emc.mongoose.api.metrics;

import com.emc.mongoose.api.common.concurrent.Coroutine;
import com.emc.mongoose.api.metrics.logging.BasicMetricsLogMessage;
import com.emc.mongoose.api.metrics.logging.ExtResultsXmlLogMessage;
import com.emc.mongoose.api.metrics.logging.MetricsAsciiTableLogMessage;
import com.emc.mongoose.api.metrics.logging.MetricsCsvLogMessage;
import com.emc.mongoose.api.model.DaemonBase;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import javax.management.MalformedObjectNameException;
import java.io.Closeable;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 18.05.17.
 */
public final class MetricsManager
extends DaemonBase
implements Coroutine {
	
	private final Map<LoadController, Map<MetricsContext, Closeable>>
		allMetrics = new HashMap<>();
	private final Set<MetricsContext> selectedMetrics = new TreeSet<>();
	private final Lock allMetricsLock = new ReentrantLock();
	
	private long outputPeriodMillis;
	private long lastOutputTs;
	private long nextOutputTs;

	private MetricsManager() {
		svcCoroutines.add(this);
	}
	
	private static final String CLS_NAME = MetricsManager.class.getSimpleName();
	private static final MetricsManager INSTANCE;

	static {

		try {
			INSTANCE = new MetricsManager();
			INSTANCE.start();
		} catch(final Throwable cause) {
			throw new AssertionError(cause);
		}
	}
	
	public static void register(final LoadController controller, final MetricsContext metricsCtx)
	throws InterruptedException, TimeoutException {
		if(INSTANCE.allMetricsLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
			try(
				final Instance stepIdCtx = CloseableThreadContext
					.put(KEY_TEST_STEP_ID, metricsCtx.getStepId())
			) {
				final Map<MetricsContext, Closeable>
					controllerMetrics = INSTANCE.allMetrics.computeIfAbsent(
						controller, c -> new HashMap<>()
					);
				controllerMetrics.put(metricsCtx, new Meter(metricsCtx));
				Loggers.MSG.info("Metrics context \"{}\" registered", metricsCtx);
			} catch(final MalformedObjectNameException e) {
				LogUtil.exception(
					Level.WARN, e,
					"Failed to register the MBean for the metrics context \"{}\"",
					metricsCtx.toString()
				);
			} finally {
				INSTANCE.allMetricsLock.unlock();
			}
		} else {
			throw new TimeoutException("Locking timeout at register call");
		}
	}
	
	public static void unregister(final LoadController controller, final MetricsContext metricsCtx)
	throws InterruptedException, TimeoutException {
		if(INSTANCE.allMetricsLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
			try(
				final Instance stepIdCtx = CloseableThreadContext
					.put(KEY_TEST_STEP_ID, metricsCtx.getStepId())
			) {
				final Map<MetricsContext, Closeable>
					controllerMetrics = INSTANCE.allMetrics.get(controller);
				if(controllerMetrics != null) {
					metricsCtx.refreshLastSnapshot(); // last time
					// check for the metrics threshold state if entered
					if(
						metricsCtx.isThresholdStateEntered() && !metricsCtx.isThresholdStateExited()
					) {
						exitMetricsThresholdState(metricsCtx);
					}
					// file output
					if(metricsCtx.getSumPersistFlag()) {
						Loggers.METRICS_FILE_TOTAL.info(new MetricsCsvLogMessage(metricsCtx));
					}
					if(metricsCtx.getPerfDbResultsFileFlag()) {
						Loggers.METRICS_EXT_RESULTS_FILE.info(
							new ExtResultsXmlLogMessage(metricsCtx));
					}
					// console output
					Loggers.METRICS_STD_OUT.info(new BasicMetricsLogMessage(metricsCtx));
					final Closeable meterMBean = controllerMetrics.remove(metricsCtx);
					if(meterMBean != null) {
						try {
							meterMBean.close();
						} catch(final IOException e) {
							LogUtil.exception(Level.WARN, e, "Failed to close the meter MBean");
						}
					}
				} else {
					Loggers.ERR.debug("Metrics context \"{}\" has not been registered", metricsCtx);
				}
				if(controllerMetrics != null && controllerMetrics.size() == 0) {
					INSTANCE.allMetrics.remove(controller);
				}
			} finally {
				INSTANCE.allMetricsLock.unlock();
				Loggers.MSG.info("Metrics context \"{}\" unregistered", metricsCtx);
			}
		} else {
			throw new TimeoutException("Locking timeout at unregister call");
		}
	}
	
	@Override
	public final void run() {
		if(allMetricsLock.tryLock()) {
			try(final Instance logCtx = CloseableThreadContext.put(KEY_CLASS_NAME, CLS_NAME)) {
				int controllerActiveTaskCount;
				int nextConcurrencyThreshold;
				for(final LoadController controller : allMetrics.keySet()) {
					if(controller.isInterrupted() || controller.isClosed()) {
						continue;
					}
					controllerActiveTaskCount = controller.getActiveTaskCount();
					for(final MetricsContext metricsCtx : allMetrics.get(controller).keySet()) {
						try(
							final Instance stepIdCtx = CloseableThreadContext.put(
								KEY_TEST_STEP_ID, metricsCtx.getStepId()
							)
						) {
							metricsCtx.refreshLastSnapshot();

							// threshold load state checks
							nextConcurrencyThreshold = metricsCtx.getConcurrencyThreshold();
							if(
								nextConcurrencyThreshold > 0 &&
									controllerActiveTaskCount >= nextConcurrencyThreshold
							) {
								if(
									!metricsCtx.isThresholdStateEntered() &&
										!metricsCtx.isThresholdStateExited()
								) {
									Loggers.MSG.info(
										"{}: the threshold of {} active tasks count is " +
										"reached, starting the additional metrics accounting",
										metricsCtx.toString(),
										metricsCtx.getConcurrencyThreshold()
									);
									metricsCtx.enterThresholdState();
								}
							} else if(
								metricsCtx.isThresholdStateEntered() &&
									!metricsCtx.isThresholdStateExited()
							) {
								exitMetricsThresholdState(metricsCtx);
							}

							// periodic output
							outputPeriodMillis = metricsCtx.getOutputPeriodMillis();
							lastOutputTs = metricsCtx.getLastOutputTs();
							nextOutputTs = System.currentTimeMillis();
							if(
								outputPeriodMillis > 0 &&
									nextOutputTs - lastOutputTs >= outputPeriodMillis
							) {
								if(!controller.isInterrupted() && !controller.isClosed()) {
									selectedMetrics.add(metricsCtx);
									metricsCtx.setLastOutputTs(nextOutputTs);
									if(metricsCtx.getAvgPersistFlag()) {
										Loggers.METRICS_FILE.info(
											new MetricsCsvLogMessage(metricsCtx)
										);
									}
								}
							}
						}
					}
				}

				// periodic console output
				if(!selectedMetrics.isEmpty()) {
					Loggers.METRICS_STD_OUT.info(new MetricsAsciiTableLogMessage(selectedMetrics));
					selectedMetrics.clear();
				}
			} catch(final Throwable cause) {
				LogUtil.exception(Level.WARN, cause, "Metrics manager failure");
			} finally {
				allMetricsLock.unlock();
			}
		}
	}

	private static void exitMetricsThresholdState(final MetricsContext metricsCtx) {
		Loggers.MSG.info(
			"{}: the active tasks count is below the threshold of {}, " +
				"stopping the additional metrics accounting",
			metricsCtx.toString(), metricsCtx.getConcurrencyThreshold()
		);
		final MetricsContext lastThresholdMetrics = metricsCtx.getThresholdMetrics();
		if(lastThresholdMetrics.getSumPersistFlag()) {
			Loggers.METRICS_THRESHOLD_FILE_TOTAL.info(
				new MetricsCsvLogMessage(lastThresholdMetrics)
			);
		}
		if(lastThresholdMetrics.getPerfDbResultsFileFlag()) {
			Loggers.METRICS_THRESHOLD_EXT_RESULTS_FILE.info(
				new ExtResultsXmlLogMessage(lastThresholdMetrics)
			);
		}
		metricsCtx.exitThresholdState();
	}
	
	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		if(isStarted() || isShutdown()) {
			state.wait(timeUnit.toMillis(timeout));
		}
		return !(isStarted() || isShutdown());
	}
	
	@Override
	protected final void doShutdown() {
	}
	
	@Override
	protected final void doInterrupt() {
		svcCoroutines.remove(this);
		try { // obtain the lock to prevent further execution
			if(!allMetricsLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				Loggers.ERR.warn("Locking timeout at interrupt call");
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "Failed to interrupt");
		}
	}
	
	@Override
	protected final void doClose() {
		for(final LoadController controller : allMetrics.keySet()) {
			allMetrics.get(controller).clear();
		}
		allMetrics.clear();
	}
}
