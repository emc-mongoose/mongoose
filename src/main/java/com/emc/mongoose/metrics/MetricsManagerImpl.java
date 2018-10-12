package com.emc.mongoose.metrics;

import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.logging.ExtResultsXmlLogMessage;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.logging.MetricsAsciiTableLogMessage;
import com.emc.mongoose.logging.MetricsCsvLogMessage;
import com.emc.mongoose.logging.StepResultsMetricsLogMessage;
import com.emc.mongoose.metrics.context.DistributedMetricsContext;
import com.emc.mongoose.metrics.context.MetricsContext;
import com.emc.mongoose.metrics.util.PrometheusMetricsExporter;
import com.github.akurilov.fiber4j.ExclusiveFiberBase;
import com.github.akurilov.fiber4j.Fiber;
import com.github.akurilov.fiber4j.FibersExecutor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;
import static com.emc.mongoose.Constants.METRIC_LABEL_CONC;
import static com.emc.mongoose.Constants.METRIC_LABEL_ID;
import static com.emc.mongoose.Constants.METRIC_LABEL_NODE;
import static com.emc.mongoose.Constants.METRIC_LABEL_OP_TYPE;
import static com.emc.mongoose.Constants.METRIC_LABEL_SIZE;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;

/**
 Created by kurila on 18.05.17.
 */
public class MetricsManagerImpl
	extends ExclusiveFiberBase
	implements MetricsManager {

	private static final String CLS_NAME = MetricsManagerImpl.class.getSimpleName();
	private final Set<MetricsContext> allMetrics = new ConcurrentSkipListSet<>();
	private final Map<DistributedMetricsContext, PrometheusMetricsExporter> distributedMetrics =
		new ConcurrentHashMap<>();
	private final Set<MetricsContext> selectedMetrics = new TreeSet<>();
	private final Lock outputLock = new ReentrantLock();
	private long outputPeriodMillis;
	private long lastOutputTs;
	private long nextOutputTs;

	public MetricsManagerImpl(final FibersExecutor instance) {
		super(instance);
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {
		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);
		int actualConcurrency;
		int nextConcurrencyThreshold;
		if(outputLock.tryLock()) {
			try {
				for(final MetricsContext metricsCtx : allMetrics) {
					ThreadContext.put(KEY_STEP_ID, metricsCtx.id());
					metricsCtx.refreshLastSnapshot();
					actualConcurrency = new Double(metricsCtx.lastSnapshot().concurrencySnapshot().mean()).intValue();
					// threshold load state checks
					nextConcurrencyThreshold = metricsCtx.concurrencyThreshold();
					if(nextConcurrencyThreshold > 0 && actualConcurrency >= nextConcurrencyThreshold) {
						if(! metricsCtx.thresholdStateEntered() && ! metricsCtx.thresholdStateExited()) {
							Loggers.MSG.info(
								"{}: the threshold of {} active load operations count is reached, " +
									"starting the additional metrics accounting",
								metricsCtx.toString(), metricsCtx.concurrencyThreshold()
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
				// console output
				if(! selectedMetrics.isEmpty()) {
					Loggers.METRICS_STD_OUT.info(new MetricsAsciiTableLogMessage(selectedMetrics));
					selectedMetrics.clear();
				}
			} catch(final ConcurrentModificationException ignored) {
			} catch(final Throwable cause) {
				LogUtil.exception(Level.DEBUG, cause, "Metrics manager failure");
			} finally {
				outputLock.unlock();
			}
		}
	}

	@Override
	public void register(final MetricsContext metricsCtx) {
		try(
			final Instance logCtx = put(KEY_STEP_ID, metricsCtx.id())
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			if(! isStarted()) {
				start();
				Loggers.MSG.debug("Started the metrics manager fiber");
			}
			allMetrics.add(metricsCtx);
			if(metricsCtx instanceof DistributedMetricsContext) {
				final DistributedMetricsContext distributedMetricsCtx = (DistributedMetricsContext) metricsCtx;
				final String[] labelNames =
					{ METRIC_LABEL_ID, METRIC_LABEL_OP_TYPE, METRIC_LABEL_CONC, METRIC_LABEL_NODE, METRIC_LABEL_SIZE };
				final String[] labelValues = {
					metricsCtx.id(),
					metricsCtx.opType().name(),
					String.valueOf(metricsCtx.concurrencyLimit()),
					String.valueOf(((DistributedMetricsContext) metricsCtx).nodeCount()),
					metricsCtx.itemDataSize().toString()
				};
				distributedMetrics.put(
					distributedMetricsCtx,
					new PrometheusMetricsExporter(distributedMetricsCtx)
						.labels(labelNames, labelValues)
						.register()
				);
			}
			Loggers.MSG.debug("Metrics context \"{}\" registered", metricsCtx);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to register the Prometheus Exporter for the metrics context \"{}\"",
				metricsCtx.toString()
			);
			e.printStackTrace();
		}
	}

	@Override
	public void unregister(final MetricsContext metricsCtx) {
		try(
			final Instance logCtx = put(KEY_STEP_ID, metricsCtx.id())
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			if(allMetrics.remove(metricsCtx)) {
				try {
					if(! outputLock.tryLock(Fiber.WARN_DURATION_LIMIT_NANOS, TimeUnit.NANOSECONDS)) {
						Loggers.ERR.warn(
							"Acquire lock timeout while unregistering the metrics context \"{}\"", metricsCtx
						);
					}
					metricsCtx.refreshLastSnapshot(); // one last time
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
					if(metricsCtx instanceof DistributedMetricsContext) {
						final DistributedMetricsContext distributedMetricsCtx = (DistributedMetricsContext) metricsCtx;
						Loggers.METRICS_STD_OUT.info(
							new MetricsAsciiTableLogMessage(Collections.singleton(metricsCtx))
						);
						Loggers.METRICS_STD_OUT.info(
							new StepResultsMetricsLogMessage(distributedMetricsCtx)
						);
					}
				} catch(final InterruptedException e) {
					throw new InterruptRunException(e);
				} finally {
					try {
						outputLock.unlock();
					} catch(final IllegalMonitorStateException ignored) {
					}
				}
			} else {
				Loggers.ERR.debug("Metrics context \"{}\" has not been registered", metricsCtx);
			}
			Loggers.MSG.debug("Metrics context \"{}\" unregistered", metricsCtx);
		} finally {
			if(allMetrics.size() == 0) {
				stop();
				Loggers.MSG.debug("Stopped the metrics manager fiber");
			}
		}
	}

	private static void exitMetricsThresholdState(final MetricsContext metricsCtx) {
		Loggers.MSG.info(
			"{}: the active load operations count is below the threshold of {}, stopping the additional metrics " +
				"accounting", metricsCtx.toString(), metricsCtx.concurrencyThreshold()
		);
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
	protected final void doClose() {
		allMetrics.forEach(MetricsContext::close);
		allMetrics.clear();
		distributedMetrics.clear();
	}
}
