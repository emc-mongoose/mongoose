package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

public abstract class StepBase
extends AsyncRunnableBase
implements Step {

	protected final Config baseConfig;
	protected final List<Map<String, Object>> stepConfigs;
	protected final Map<String, String> env;
	protected String id;

	protected StepBase(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env
	) {
		this.baseConfig = baseConfig;
		this.stepConfigs = stepConfigs;
		this.env = env;
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		final Config actualConfig = initConfig();
		final String stepId = actualConfig.getTestConfig().getStepConfig().getId();
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			doStart(actualConfig);
		} catch(final InterruptedException e) {
			throw new CancellationException();
		} catch(final Throwable cause) {
			LogUtil.exception(Level.WARN, cause, "{} step failed", id);
		} finally {
			try {
				close();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "{} step failed to close", id);
			}
		}
	}

	protected Config initConfig() {
		final String autoStepId = getTypeName() + "_" + LogUtil.getDateTimeStamp() + "_"
			+ hashCode();
		final Config config = new Config(baseConfig);
		if(stepConfigs != null && stepConfigs.size() > 0) {
			for(final Map<String, Object> nextStepConfig: stepConfigs) {
				config.apply(nextStepConfig, autoStepId);
			}
		}
		id = config.getTestConfig().getStepConfig().getId();
		return config;
	}

	protected abstract void doStart(final Config actualConfig)
	throws InterruptedException;

	@Override
	protected abstract void doStop();

	@Override
	protected abstract void doClose();

	protected abstract String getTypeName();

	@Override
	public StepBase config(final Object config)
	throws ScenarioParseException {
		if(config instanceof Map) {
			final List<Map<String, Object>> stepConfigsCopy = new ArrayList<>();
			if(stepConfigs != null) {
				stepConfigsCopy.addAll(stepConfigs);
			}
			stepConfigsCopy.add((Map<String, Object>) config);
			return copyInstance(stepConfigsCopy);
		} else {
			return copyInstance(config);
		}
	}

	protected abstract StepBase copyInstance(final Object config);
}
