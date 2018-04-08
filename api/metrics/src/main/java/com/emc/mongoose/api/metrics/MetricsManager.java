package com.emc.mongoose.api.metrics;

import com.emc.mongoose.api.model.concurrent.DaemonBase;
import com.emc.mongoose.api.model.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.api.metrics.logging.BasicMetricsLogMessage;
import com.emc.mongoose.api.metrics.logging.ExtResultsXmlLogMessage;
import com.emc.mongoose.api.metrics.logging.MetricsAsciiTableLogMessage;
import com.emc.mongoose.api.metrics.logging.MetricsCsvLogMessage;
import com.emc.mongoose.api.model.concurrent.ThreadDump;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import com.github.akurilov.concurrent.coroutines.Coroutine;
import com.github.akurilov.concurrent.coroutines.CoroutineBase;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import javax.management.MalformedObjectNameException;
import java.io.Closeable;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

	private final Map<String, Map<MetricsContext, Closeable>> allMetrics = new HashMap<>();
	private final Set<MetricsContext> selectedMetrics = new TreeSet<>();
	private final Lock allMetricsLock = new ReentrantLock();

	private final Coroutine coroutine = new CoroutineBase(ServiceTaskExecutor.INSTANCE) {

		private long outputPeriodMillis;
		private long lastOutputTs;
		private long nextOutputTs;

		@Override
		protected final void doClose() {
		}

		@Override
		protected final void invokeTimed(final long startTimeNanos) {
			if(allMetricsLock.tryLock()) {

				ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);

				try {
					int actualConcurrency;
					int nextConcurrencyThreshold;
					for(final var id : allMetrics.keySet()) {
						for(final var metricsCtx : allMetrics.get(id).keySet()) {

							ThreadContext.put(KEY_TEST_STEP_ID, metricsCtx.getStepId());

							actualConcurrency = metricsCtx.getActualConcurrency();
							metricsCtx.refreshLastSnapshot();

							// threshold load state checks
							nextConcurrencyThreshold = metricsCtx.getConcurrencyThreshold();
							if(
								nextConcurrencyThreshold > 0 &&
									actualConcurrency >= nextConcurrencyThreshold
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
								outputPeriodMillis > 0
									&& nextOutputTs - lastOutputTs >= outputPeriodMillis
							) {
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

					// periodic console output
					if(!selectedMetrics.isEmpty()) {
						Loggers.METRICS_STD_OUT.info(
							new MetricsAsciiTableLogMessage(selectedMetrics)
						);
						selectedMetrics.clear();
					}
				} catch(final Throwable cause) {
					LogUtil.exception(Level.WARN, cause, "Metrics manager failure");
				} finally {
					allMetricsLock.unlock();
				}
			}
		}
	};
	
	private MetricsManager() {
	}

	public static void register(final String id, final MetricsContext metricsCtx)
	throws InterruptedException {
		if(INSTANCE.allMetricsLock.tryLock(1, TimeUnit.SECONDS)) {
			if(!INSTANCE.isStarted()) {
				INSTANCE.start();
				Loggers.MSG.info("Started the metrics manager coroutine");
			}
			try(final var logCtx = CloseableThreadContext.put(KEY_TEST_STEP_ID, id)) {
				final var stepMetrics = INSTANCE.allMetrics.computeIfAbsent(
					id, c -> new HashMap<>()
				);
				stepMetrics.put(metricsCtx, new Meter(metricsCtx));
				Loggers.MSG.debug("Metrics context \"{}\" registered", metricsCtx);
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
			Loggers.ERR.warn(
				"Locking timeout at register call, thread dump:\n{}", new ThreadDump().toString()
			);
		}
	}
	
	public static void unregister(final String id, final MetricsContext metricsCtx)
	throws InterruptedException {
		if(INSTANCE.allMetricsLock.tryLock(1, TimeUnit.SECONDS)) {
			try(final var stepIdCtx = CloseableThreadContext.put(KEY_TEST_STEP_ID, id)) {
				final var stepMetrics = INSTANCE.allMetrics.get(id);
				if(stepMetrics != null) {
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
							new ExtResultsXmlLogMessage(metricsCtx)
						);
					}
					// console output
					Loggers.METRICS_STD_OUT.info(
						new MetricsAsciiTableLogMessage(Collections.singleton(metricsCtx), true)
					);
					Loggers.METRICS_STD_OUT.info(new BasicMetricsLogMessage(metricsCtx));
					final var meterMBean = stepMetrics.remove(metricsCtx);
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
				if(stepMetrics != null && stepMetrics.size() == 0) {
					INSTANCE.allMetrics.remove(id);
				}
			} finally {
				if(INSTANCE.allMetrics.size() == 0) {
					try {
						INSTANCE.stop();
						Loggers.MSG.info("Stopped the metrics manager coroutine");
					} catch(final RemoteException ignored) {
					}
				}
				INSTANCE.allMetricsLock.unlock();
				Loggers.MSG.debug("Metrics context \"{}\" unregistered", metricsCtx);
			}
		} else {
			Loggers.ERR.warn(
				"Locking timeout at unregister call, thread dump:\n{}", new ThreadDump().toString()
			);
		}
	}

	private static void exitMetricsThresholdState(final MetricsContext metricsCtx) {
		Loggers.MSG.info(
			"{}: the active tasks count is below the threshold of {}, " +
				"stopping the additional metrics accounting",
			metricsCtx.toString(), metricsCtx.getConcurrencyThreshold()
		);
		final var lastThresholdMetrics = metricsCtx.getThresholdMetrics();
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
	protected final void doStart() {
		try {
			if(allMetricsLock.tryLock(1, TimeUnit.SECONDS)) {
				try {
					coroutine.start();
				} catch(final RemoteException ignored) {
				} finally {
					allMetricsLock.unlock();
				}
			} else {
				Loggers.ERR.warn(
					"Locking timeout at starting, thread dump:\n{}", new ThreadDump().toString()
				);
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.DEBUG, e, "Got interrupted exception");
		}
	}

	@Override
	protected final void doShutdown() {
	}

	@Override
	protected final void doStop() {
		try {
			if(allMetricsLock.tryLock(1, TimeUnit.SECONDS)) {
				try {
					coroutine.stop();
				} catch(final RemoteException ignored) {
				} finally {
					allMetricsLock.unlock();
				}
			} else {
				Loggers.ERR.warn(
					"Locking timeout at stopping, thread dump:\n{}", new ThreadDump().toString()
				);
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.DEBUG, e, "Got interrupted exception");
		}
	}
	
	@Override
	protected final void doClose() {
		try {
			if(allMetricsLock.tryLock(1, TimeUnit.SECONDS)) {
				try {
					for(final var id : allMetrics.keySet()) {
						allMetrics.get(id).clear();
					}
					allMetrics.clear();
				} finally {
					allMetricsLock.unlock();
				}
			} else {
				Loggers.ERR.warn(
					"Locking timeout at closing, thread dump:\n{}", new ThreadDump().toString()
				);
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.DEBUG, e, "Got interrupted exception");
		}
	}
}
