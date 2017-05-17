package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

/**
 Created by kurila on 08.04.16.
 */
public abstract class StepBase
implements Step {

	protected final Config localConfig;

	protected StepBase(final Config appConfig) {
		localConfig = new Config(appConfig);
	}

	@Override
	public final Config getConfig() {
		return localConfig;
	}
	
	@Override
	public void run() {

		final StepConfig stepConfig = localConfig.getTestConfig().getStepConfig();
		String stepName = stepConfig.getName();
		if(stepName == null) {
			stepName = ThreadContext.get(KEY_STEP_NAME);
			if(stepName == null) {
				Loggers.ERR.fatal("Step name is not set");
			} else {
				stepConfig.setName(stepName);
			}
		}

		try(
			final CloseableThreadContext.Instance ctx = CloseableThreadContext
				.put(KEY_STEP_NAME, stepName)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			Loggers.CONFIG.info(localConfig.toString());
			invoke();
		} catch(final Throwable cause) {
			LogUtil.exception(Level.ERROR, cause, "Test step failure");
			cause.printStackTrace(System.err);
		}
	}

	protected abstract void invoke();
}
