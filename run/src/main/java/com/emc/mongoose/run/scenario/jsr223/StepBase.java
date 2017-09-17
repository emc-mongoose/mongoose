package com.emc.mongoose.run.scenario.jsr223;

import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 The scenario step base containing the immutable configuration.
 */
public abstract class StepBase
implements Step {

	protected final Config baseConfig;
	protected final Map<String, Object> stepConfig;

	protected StepBase(final Config baseConfig) {
		this(baseConfig, null);
	}

	protected StepBase(final Config baseConfig, final Map<String, Object> stepConfig) {
		this.baseConfig = baseConfig;
		this.stepConfig = stepConfig;
	}

	@Override
	public StepBase config(final Map<String, Object> stepConfig)
	throws ScenarioParseException {
		return copyInstance(stepConfig);
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

	@Override
	public void close()
	throws IOException {
		if(stepConfig != null) {
			stepConfig.clear();
		}
	}

	protected Config init()
	throws ScenarioParseException {
		final String autoStepId = getTypeName() + "_" + LogUtil.getDateTimeStamp() + "_"
			+ hashCode();
		final Config config = new Config(baseConfig);
		if(stepConfig != null) {
			config.apply(stepConfig, autoStepId);
		}
		return config;
	}

	protected abstract void invoke(final Config actualConfig)
	throws Throwable;

	protected abstract StepBase copyInstance(final Map<String, Object> stepConfig);

	protected abstract String getTypeName();
}
