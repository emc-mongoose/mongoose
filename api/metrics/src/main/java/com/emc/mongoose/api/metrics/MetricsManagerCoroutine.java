package com.emc.mongoose.api.metrics;

import com.emc.mongoose.api.metrics.logging.ExtResultsXmlLogMessage;
import com.emc.mongoose.api.metrics.logging.MetricsAsciiTableLogMessage;
import com.emc.mongoose.api.metrics.logging.MetricsCsvLogMessage;
import com.emc.mongoose.api.model.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import com.github.akurilov.coroutines.CoroutineBase;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;

public final class MetricsManagerCoroutine
extends CoroutineBase {

	private static final String CLS_NAME = MetricsManagerCoroutine.class.getSimpleName();

	private final Lock allMetricsLock;
	private final Map<String, Map<MetricsContext, Closeable>> allMetrics;

	private final Set<MetricsContext> selectedMetrics = new TreeSet<>();

	private long outputPeriodMillis;
	private long lastOutputTs;
	private long nextOutputTs;

	public MetricsManagerCoroutine(
		final Lock allMetricsLock, final Map<String, Map<MetricsContext, Closeable>> allMetrics
	) {
		super(ServiceTaskExecutor.INSTANCE);
		this.allMetricsLock = allMetricsLock;
		this.allMetrics = allMetrics;
	}

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
				for(final String id : allMetrics.keySet()) {
					for(final MetricsContext metricsCtx : allMetrics.get(id).keySet()) {

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
									metricsCtx.toString(), metricsCtx.getConcurrencyThreshold()
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
}
