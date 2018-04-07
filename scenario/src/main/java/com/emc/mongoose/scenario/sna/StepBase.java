package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.metrics.MetricsContext;
import com.emc.mongoose.api.metrics.MetricsManager;
import com.emc.mongoose.api.metrics.MetricsSnapshot;
import com.emc.mongoose.api.model.concurrent.DaemonBase;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import com.emc.mongoose.ui.log.Loggers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public abstract class StepBase
extends DaemonBase
implements Step, Runnable {

	protected final Config baseConfig;
	protected final List<Map<String, Object>> stepConfigs;
	protected final Int2ObjectMap<MetricsContext> metricsByIoType = new Int2ObjectOpenHashMap<>();

	private volatile StepClient stepClient = null;
	private volatile Config actualConfig = null;
	private volatile long timeLimitSec = Long.MAX_VALUE;
	private volatile long startTimeSec = -1;
	private String id = null;
	private boolean distributedFlag = false;

	protected StepBase(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		this.baseConfig = baseConfig;
		this.stepConfigs = stepConfigs;
	}

	protected final void actualConfig(final Config actualConfig) {
		this.actualConfig = actualConfig;
		final StepConfig stepConfig = actualConfig.getTestConfig().getStepConfig();
		this.id = stepConfig.getId();
		this.distributedFlag = stepConfig.getDistributed();
	}


	@Override
	public final void run() {
		try {
			start();
			try {
				await(timeLimitSec, TimeUnit.SECONDS);
			} catch(final IllegalStateException e) {
				LogUtil.exception(Level.WARN, e, "Failed to await \"{}\"", toString());
			} catch(final InterruptedException e) {
				throw new CancellationException();
			}
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.WARN, e, "Failed to start \"{}\"", toString());
		} finally {
			try {
				close();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "Failed to close \"{}\"", toString());
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void doStart()
	throws IllegalStateException {

		init();

		final StepConfig stepConfig = actualConfig.getTestConfig().getStepConfig();
		final String stepId = stepConfig.getId();
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			if(isDistributed()) {
				stepClient = new StepClient(this, actualConfig);
				stepClient.start();
			} else {
				doStartLocal(actualConfig);
			}

			long t = stepConfig.getLimitConfig().getTime();
			if(t > 0) {
				timeLimitSec = t;
			}
			startTimeSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed to start", id);
		}

		metricsByIoType.forEach(
			(ioTypeCode, metricsCtx) -> {
				metricsCtx.start();
				try {
					MetricsManager.register(id, metricsCtx);
				} catch(final InterruptedException e) {
					throw new CancellationException(e.getMessage());
				}
			}
		);
	}

	protected abstract void init();

	protected final boolean isDistributed() {
		return distributedFlag;
	}

	protected abstract void doStartLocal(final Config actualConfig);

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		if(stepClient == null) {
			return awaitLocal(timeout, timeUnit);
		} else {
			return stepClient.await(timeout, timeUnit);
		}
	}

	protected abstract boolean awaitLocal(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException;

	@Override
	protected void doStop() {

		if(stepClient != null) {
			try {
				stepClient.stop();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "{} step failed to stop", id);
			}
		}

		metricsByIoType
			.values()
			.forEach(
				metricsCtx -> {
					try {
						MetricsManager.unregister(id(), metricsCtx);
					} catch(final InterruptedException e) {
						throw new CancellationException(e.getMessage());
					}
				}
			);

		if(stepClient == null) {
			doStopLocal();
		}

		final long t = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - startTimeSec;
		if(t < 0) {
			Loggers.ERR.warn(
				"Stopped earlier than started, won't account the elapsed time"
			);
		} else if(t > timeLimitSec) {
			Loggers.MSG.warn(
				"The elapsed time ({}[s]) is more than the limit ({}[s]), won't resume",
				t, timeLimitSec
			);
			timeLimitSec = 0;
		} else {
			timeLimitSec -= t;
		}
	}

	protected abstract void doStopLocal();

	@Override
	protected void doClose()
	throws IOException {

		metricsByIoType
			.values()
			.forEach(
				metricsCtx -> {
					try {
						metricsCtx.close();
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "Failed to close the metrics context \"{}\"", metricsCtx
						);
					}
				}
			);

		if(stepClient == null) {
			doCloseLocal();
		} else {
			stepClient.close();
		}
	}

	protected abstract void doCloseLocal();

	@Override
	public StepBase config(final Map<String, Object> config) {
		final List<Map<String, Object>> stepConfigsCopy = new ArrayList<>();
		if(stepConfigs != null) {
			stepConfigsCopy.addAll(stepConfigs);
		}
		stepConfigsCopy.add(config);
		return copyInstance(stepConfigsCopy);
	}

	@Override
	public final String id() {
		return id;
	}

	@Override
	public final int actualConcurrency() {
		if(stepClient == null) {
			return actualConcurrencyLocal();
		} else {
			return stepClient.actualConcurrency();
		}
	}

	@Override
	public final MetricsSnapshot getLastMetricsSnapshot(final int ioTypeCode) {
		return metricsByIoType.get(ioTypeCode).getLastSnapshot();
	}

	protected abstract int actualConcurrencyLocal();

	protected abstract StepBase copyInstance(final List<Map<String, Object>> stepConfigs);
}
