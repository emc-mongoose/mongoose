package com.emc.mongoose.scenario.old;

import com.emc.mongoose.scenario.ScenarioParseException;

import java.util.Map;

public interface ConfigurableStep
extends Step {

	/**
	 Configures the scenario step
	 @param stepConfig a dictionary of the configuration values to override the inherited config
	 @return <b>new</b> step with the applied config values
	 */
	Step config(final Map<String, Object> stepConfig)
	throws ScenarioParseException;
}
