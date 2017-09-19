package com.emc.mongoose.scenario.step;

import com.emc.mongoose.scenario.ScenarioParseException;

import java.io.Closeable;
import java.util.Map;

/**
 A runnable step configuration container. The collected configuration is applied upon invocation.
 */
public interface Step
extends Closeable, Runnable {

	/**
	 Configures the scenario step
	 @param stepConfig a dictionary of the configuration values to override the inherited config
	 @return <b>new</b> step with the applied config values
	 */
	Step config(final Map<String, Object> stepConfig)
	throws ScenarioParseException;
}
