package com.emc.mongoose.ui.log;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.AbstractTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import static com.emc.mongoose.common.Constants.KEY_TEST_STEP_ID;

/**
 Created by kurila on 30.06.17.
 Note that the instances are not thread safe so they will work only in case of async logger is used.
 */
@Plugin(name = "StepIdTriggeringPolicy", category = Core.CATEGORY_NAME, printObject = true)
public final class StepIdTriggeringPolicy
extends AbstractTriggeringPolicy {

	private String lastStepId = null;
	
	@PluginFactory
	public static StepIdTriggeringPolicy createPolicy() {
		return new StepIdTriggeringPolicy();
	}
	
	@Override
	public final void initialize(final RollingFileManager manager) {
	}
	
	@Override
	public final boolean isTriggeringEvent(final LogEvent logEvent) {
		final String stepId = logEvent.getContextData().getValue(KEY_TEST_STEP_ID);
		if(null == stepId) {
			return false;
		}
		ThreadContext.put(KEY_TEST_STEP_ID, stepId);
		if(stepId.equals(lastStepId)) {
			return false;
		} else {
			lastStepId = stepId;
			return true;
		}
	}
}
