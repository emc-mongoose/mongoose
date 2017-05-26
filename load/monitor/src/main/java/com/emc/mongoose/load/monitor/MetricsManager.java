package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.model.DaemonBase;
import com.emc.mongoose.model.load.LoadController;
import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 18.05.17.
 */
public final class MetricsManager
extends DaemonBase
implements SvcTask {
	
	private final Map<LoadController, SortedSet<MetricsContext>> allMetrics = new HashMap<>();
	private final Lock allMetricsLock = new ReentrantLock();
	private final long outputPeriodMillis;
	private volatile long lastOutputTs;
	private volatile long nextOutputTs;
	
	private MetricsManager(final long outputPeriodMillis) {
		svcTasks.add(this);
		this.outputPeriodMillis = outputPeriodMillis;
		lastOutputTs = System.currentTimeMillis() - outputPeriodMillis;
	}
	
	private static final String CLASS_NAME = MetricsManager.class.getName();
	private static final MetricsManager INSTANCE;
	
	static {
		try {
			final Config defaultConfig  = ConfigParser.loadDefaultConfig();
			final long defaultPeriod = defaultConfig
				.getTestConfig()
				.getStepConfig()
				.getMetricsConfig()
				.getPeriod();
			INSTANCE = new MetricsManager(TimeUnit.SECONDS.toMillis(defaultPeriod));
			INSTANCE.start();
		} catch(final Throwable cause) {
			throw new AssertionError(cause);
		}
	}
	
	public static void register(final LoadController controller, final MetricsContext metricsCtx) {
		try {
			if(INSTANCE.allMetricsLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				try {
					final SortedSet<MetricsContext> controllerMetrics = INSTANCE.allMetrics
						.computeIfAbsent(controller, c -> new TreeSet<>());
					if(controllerMetrics.add(metricsCtx)) {
						Loggers.MSG.info("Metrics context \"{}\" registered", metricsCtx);
					} else {
						Loggers.ERR.warn(
							"Metrics context \"{}\" has been registered already", metricsCtx
						);
					}
				} finally {
					INSTANCE.allMetricsLock.unlock();
				}
			} else {
				Loggers.ERR.warn("Locking timeout at register call");
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "Failed to register \"{}\"", metricsCtx);
		}
	}
	
	public static void unregister(
		final LoadController controller, final MetricsContext metricsCtx
	) {
		try {
			if(INSTANCE.allMetricsLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				try {
					final SortedSet<MetricsContext> controllerMetrics = INSTANCE.allMetrics
						.get(controller);
					if(controllerMetrics != null && controllerMetrics.remove(metricsCtx)) {
						// check for the metrics threshold state if entered
						if(
							metricsCtx.isThresholdStateEntered() &&
								!metricsCtx.isThresholdStateExited()
						) {
							exitMetricsThresholdState(metricsCtx);
						}
						// output
						Loggers.METRICS_FILE_TOTAL.info(new MetricsCsvLogMessage(metricsCtx));
						Loggers.METRICS_STD_OUT.info(new BasicMetricsLogMessage(metricsCtx));
						Loggers.METRICS_EXT_RESULTS_FILE.info(
							new ExtResultsXmlLogMessage(metricsCtx)
						);
					} else {
						Loggers.ERR.debug(
							"Metrics context \"{}\" has not been registered", metricsCtx
						);
					}
					if(controllerMetrics != null && controllerMetrics.size() == 0) {
						INSTANCE.allMetrics.remove(controller);
					}
				} finally {
					INSTANCE.allMetricsLock.unlock();
				}
			} else {
				Loggers.ERR.warn("Locking timeout at unregister call");
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "Failed to unregister \"{}\"", metricsCtx);
		}
	}
	
	@Override
	public final void run() {
		if(allMetricsLock.tryLock()) {
			try {
				nextOutputTs = System.currentTimeMillis();
				int controllerActiveTaskCount;
				for(final LoadController controller : allMetrics.keySet()) {
					controllerActiveTaskCount = controller.getActiveTaskCount();
					for(final MetricsContext metricsCtx : allMetrics.get(controller)) {
						metricsCtx.refreshLastSnapshot();
						// threshold load state checks
						if(controllerActiveTaskCount >= metricsCtx.getThresholdConcurrency()) {
							if(
								!metricsCtx.isThresholdStateEntered() &&
									!metricsCtx.isThresholdStateExited()
							) {
								Loggers.MSG.info(
									"The threshold of {} active tasks count is reached, " +
										"starting the additional metrics accounting",
									metricsCtx.getThresholdConcurrency()
								);
								metricsCtx.enterThresholdState();
							}
						} else if(
							metricsCtx.isThresholdStateEntered() &&
								!metricsCtx.isThresholdStateExited()
						) {
							exitMetricsThresholdState(metricsCtx);
						}
						// periodic file output
						if(
							nextOutputTs - metricsCtx.getLastOutputTs() >=
								metricsCtx.getOutputPeriodMillis()
						) {
							try(
								final Instance logCtx = CloseableThreadContext
									.put(KEY_STEP_NAME, metricsCtx.getStepName())
							        .put(KEY_CLASS_NAME, CLASS_NAME)
							) {
								Loggers.METRICS_FILE.info(new MetricsCsvLogMessage(metricsCtx));
							}
						}
					}
				}
				// periodic console output
				if(nextOutputTs - lastOutputTs >= outputPeriodMillis) {
					lastOutputTs = nextOutputTs;
					Loggers.METRICS_STD_OUT.info(new MetricsAsciiTableLogMessage(allMetrics));
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
			"The active tasks count is below the threshold of {}, " +
				"stopping the additional metrics accounting",
			metricsCtx.getThresholdConcurrency()
		);
		final MetricsContext lastThresholdMetrics = metricsCtx
			.getThresholdMetrics();
		Loggers.METRICS_THRESHOLD_FILE_TOTAL.info(
			new MetricsCsvLogMessage(lastThresholdMetrics)
		);
		Loggers.METRICS_THRESHOLD_EXT_RESULTS_FILE.info(
			new ExtResultsXmlLogMessage(lastThresholdMetrics)
		);
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
		svcTasks.remove(this);
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
