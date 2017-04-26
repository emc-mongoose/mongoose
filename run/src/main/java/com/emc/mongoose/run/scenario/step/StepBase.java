package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 Created by kurila on 08.04.16.
 */
public abstract class StepBase
implements Step {

	private static final Logger LOG = LogManager.getLogger();

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
		String jobName = stepConfig.getName();
		if(jobName == null) {
			jobName = ThreadContext.get(KEY_STEP_NAME);
			if(jobName == null) {
				LOG.fatal(Markers.ERR, "Step name is not set");
			} else {
				stepConfig.setName(jobName);
			}
		}

		try(
			final CloseableThreadContext.Instance ctx = CloseableThreadContext.put(
				KEY_STEP_NAME, jobName
			)
		) {
			LOG.info(Markers.CFG, localConfig.toString());
			invoke();
		} catch(final Throwable cause) {
			LogUtil.exception(LOG, Level.ERROR, cause, "Test step failure");
		}
	}

	protected abstract void invoke();
}
