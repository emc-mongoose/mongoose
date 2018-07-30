package com.emc.mongoose.metrics;

import com.emc.mongoose.concurrent.DaemonBase;
import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.logging.ExtResultsXmlLogMessage;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.logging.MetricsAsciiTableLogMessage;
import com.emc.mongoose.logging.MetricsCsvLogMessage;
import com.emc.mongoose.logging.StepResultsMetricsLogMessage;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

import com.github.akurilov.commons.concurrent.AsyncRunnable;

import com.github.akurilov.fiber4j.ExclusiveFiberBase;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 Created by kurila on 18.05.17.
 */
public final class MetricsManager
	extends DaemonBase {

	private static final String CLS_NAME = MetricsManager.class.getSimpleName();
	private static final MetricsManager INSTANCE;

	static {
		try {
			INSTANCE = new MetricsManager();
		} catch(final Throwable cause) {
			throw new AssertionError(cause);
		}
	}

	private final Map<String, Map<MetricsContext, AutoCloseable>> allMetrics = new ConcurrentHashMap<>();
	private final Set<MetricsContext> selectedMetrics = new TreeSet<>();
	private final AsyncRunnable task = new ExclusiveFiberBase(ServiceTaskExecutor.INSTANCE) {
		private long outputPeriodMillis;
		private long lastOutputTs;
		private long nextOutputTs;

		@Override
		protected final void doClose() {
		}

		@Override
		protected final void invokeTimedExclusively(final long startTimeNanos) {

			ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);

			try {
				for(final String id : allMetrics.keySet()) {
					for(final MetricsContext metricsCtx : allMetrics.get(id).keySet()) {
						metricsCtx.refreshLastSnapshot();
					}
				}
			} catch(final ConcurrentModificationException ignored) {
			}

			try {
				int actualConcurrency;
				int nextConcurrencyThreshold;
				for(final String id : allMetrics.keySet()) {
					for(final MetricsContext metricsCtx : allMetrics.get(id).keySet()) {
						ThreadContext.put(KEY_STEP_ID, metricsCtx.stepId());
						actualConcurrency = metricsCtx.lastSnapshot().actualConcurrencyLast();
						//metricsCtx.refreshLastSnapshot();
						// threshold load state checks
						nextConcurrencyThreshold = metricsCtx.concurrencyThreshold();
						if(nextConcurrencyThreshold > 0 && actualConcurrency >= nextConcurrencyThreshold) {
							if(!metricsCtx.thresholdStateEntered() && !metricsCtx.thresholdStateExited()) {
								Loggers.MSG.info(
									"{}: the threshold of {} active load operations count is reached, " +
										"starting the additional metrics accounting", metricsCtx.toString(),
									metricsCtx.concurrencyThreshold()
								);
								metricsCtx.enterThresholdState();
							}
						} else if(metricsCtx.thresholdStateEntered() && ! metricsCtx.thresholdStateExited()) {
							exitMetricsThresholdState(metricsCtx);
						}
						// periodic output
						outputPeriodMillis = metricsCtx.outputPeriodMillis();
						lastOutputTs = metricsCtx.lastOutputTs();
						nextOutputTs = System.currentTimeMillis();
						if(outputPeriodMillis > 0 && nextOutputTs - lastOutputTs >= outputPeriodMillis) {
							selectedMetrics.add(metricsCtx);
							metricsCtx.lastOutputTs(nextOutputTs);
							if(metricsCtx.avgPersistEnabled()) {
								Loggers.METRICS_FILE.info(new MetricsCsvLogMessage(metricsCtx));
							}
						}
					}
				}
				// periodic console output
				if(! selectedMetrics.isEmpty()) {
					Loggers.METRICS_STD_OUT.info(new MetricsAsciiTableLogMessage(selectedMetrics));
					selectedMetrics.clear();
				}
			} catch(final Throwable cause) {
				LogUtil.exception(Level.DEBUG, cause, "Metrics manager failure");
			}
		}
	};

	private MetricsManager() {
	}

	public static void register(final String id, final MetricsContext metricsCtx)
	throws InterruptedException {
		try(
			final Instance logCtx = put(KEY_STEP_ID, id)
				.put(KEY_CLASS_NAME, MetricsManager.class.getSimpleName())
		) {
			if(!INSTANCE.isStarted()) {
				INSTANCE.start();
				Loggers.MSG.debug("Started the metrics manager fiber");
			}
			final Map<MetricsContext, AutoCloseable> stepMetrics = INSTANCE.allMetrics.computeIfAbsent(
				id, c -> new ConcurrentHashMap<>()
			);
			stepMetrics.put(metricsCtx, new Meter(metricsCtx));
			Loggers.MSG.debug("Metrics context \"{}\" registered", metricsCtx);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to register the MBean for the metrics context \"{}\"", metricsCtx.toString()
			);
		}
	}

	public static void unregister(final String id, final MetricsContext metricsCtx)
	throws InterruptedException {
		try(
			final Instance stepIdCtx = put(KEY_STEP_ID, id)
				.put(KEY_CLASS_NAME, MetricsManager.class.getSimpleName())
		) {
			final Map<MetricsContext, AutoCloseable> stepMetrics = INSTANCE.allMetrics.get(id);
			if(stepMetrics != null) {
				metricsCtx.refreshLastSnapshot(); // last time
				// check for the metrics threshold state if entered
				if(metricsCtx.thresholdStateEntered() && ! metricsCtx.thresholdStateExited()) {
					exitMetricsThresholdState(metricsCtx);
				}
				// file output
				if(metricsCtx.sumPersistEnabled()) {
					Loggers.METRICS_FILE_TOTAL.info(new MetricsCsvLogMessage(metricsCtx));
				}
				if(metricsCtx.perfDbResultsFileEnabled()) {
					Loggers.METRICS_EXT_RESULTS_FILE.info(new ExtResultsXmlLogMessage(metricsCtx));
				}
				// console output
				Loggers.METRICS_STD_OUT.info(
					new MetricsAsciiTableLogMessage(Collections.singleton(metricsCtx), true));
				Loggers.METRICS_STD_OUT.info(new StepResultsMetricsLogMessage(metricsCtx));
				final AutoCloseable meterMBean = stepMetrics.remove(metricsCtx);
				if(meterMBean != null) {
					try {
						meterMBean.close();
					} catch(final Exception e) {
						LogUtil.exception(Level.WARN, e, "Failed to close the meter MBean");
					}
				}
			} else {
				Loggers.ERR.debug("Metrics context \"{}\" has not been registered", metricsCtx);
			}
			if(stepMetrics != null && stepMetrics.size() == 0) {
				INSTANCE.allMetrics.remove(id);
			}
		} finally {
			if(INSTANCE.allMetrics.size() == 0) {
				INSTANCE.stop();
				Loggers.MSG.debug("Stopped the metrics manager fiber");
			}
			Loggers.MSG.debug("Metrics context \"{}\" unregistered", metricsCtx);
		}
	}

	private static void exitMetricsThresholdState(final MetricsContext metricsCtx) {
		Loggers.MSG.info(
			"{}: the active load operations count is below the threshold of {}, stopping the additional metrics " +
				"accounting", metricsCtx.toString(), metricsCtx.concurrencyThreshold());
		final MetricsContext lastThresholdMetrics = metricsCtx.thresholdMetrics();
		if(lastThresholdMetrics.sumPersistEnabled()) {
			Loggers.METRICS_THRESHOLD_FILE_TOTAL.info(new MetricsCsvLogMessage(lastThresholdMetrics));
		}
		if(lastThresholdMetrics.perfDbResultsFileEnabled()) {
			Loggers.METRICS_THRESHOLD_EXT_RESULTS_FILE.info(new ExtResultsXmlLogMessage(lastThresholdMetrics));
		}
		metricsCtx.exitThresholdState();
	}

	@Override
	protected final void doStart() {
		try {
			task.start();
		} catch(final RemoteException ignored) {
		}
	}

	@Override
	protected final void doShutdown() {
	}

	@Override
	protected final void doStop() {
		try {
			task.stop();
		} catch(final RemoteException e) {
		}
	}

	@Override
	protected final void doClose() {
		for(final Map<MetricsContext, AutoCloseable> meters : allMetrics.values()) {
			meters.clear();
		}
		allMetrics.clear();
	}
}
