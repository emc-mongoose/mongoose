package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.common.Constants.KEY_STEP_ID;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;

import java.util.concurrent.CancellationException;

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
		String stepId = stepConfig.getId();
		if(stepId == null) {
			stepId = LogUtil.getDateTimeStamp();
			Loggers.MSG.info("Auto-generated the test step id \"{}\" for \"{}\"", stepId, this);
			stepConfig.setId(stepId);
		} else {
			Loggers.MSG.info("Run the test step \"{}\" with id \"{}\"", this, stepId);
		}

		try(
			final CloseableThreadContext.Instance ctx = CloseableThreadContext
				.put(KEY_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			Loggers.CONFIG.info(localConfig.toString());
			invoke();
		}
	}

	protected abstract void invoke()
	throws CancellationException;
}
