package com.emc.mongoose.run.scenario.jsr223;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 The scenario step base containing the immutable configuration.
 */
public abstract class StepBase
implements Step {

	protected final Config baseConfig;
	protected final Map<String, Object> stepConfig;
	protected final CompositeStep parentStep;

	protected StepBase(final Config baseConfig) {
		this(baseConfig, null, null);
	}

	protected StepBase(
		final Config baseConfig, final Map<String, Object> stepConfig, final CompositeStep parentStep
	) {
		this.baseConfig = baseConfig;
		this.stepConfig = stepConfig;
		this.parentStep = parentStep;
	}

	@Override
	public StepBase config(final Map<String, Object> stepConfig) {
		return copyInstance(baseConfig, stepConfig, parentStep);
	}

	@Override
	public StepBase parent(final CompositeStep parentStep) {
		return copyInstance(baseConfig, stepConfig, parentStep);
	}

	@Override
	public Map<String, Object> getStepConfig() {
		if(parentStep != null) {
			final Map<String, Object> parentStepConfig = parentStep.getStepConfig();
			final Map<String, Object> mergedStepConfig = new HashMap<>();
			// TODO deep copy stepConfig to mergedStepConfig
			// TODO deep merge parentStepConfig to mergedStepConfig
			return mergedStepConfig;
		} else {
			return stepConfig;
		}
	}

	@Override
	public final void run() {
		try {
			invoke(init());
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed", getTypeName());
		}
	}

	protected Config init() {
		final String autoStepId = getTypeName() + "_" + LogUtil.getDateTimeStamp() + "_"
			+ hashCode();
		final Map<String, Object> mergedStepConfig = getStepConfig();
		final Config config = new Config(baseConfig);
		if(mergedStepConfig != null) {
			config.apply(mergedStepConfig, autoStepId);
		}
		return config;
	}

	protected abstract void invoke(final Config actualConfig)
	throws Throwable;

	protected abstract StepBase copyInstance(
		final Config configCopy, final Map<String, Object> stepConfig,
		final CompositeStep parentStep
	);

	protected abstract String getTypeName();
}
