package com.emc.mongoose.run.scenario.jsr223;

import java.util.Map;

/**
 A runnable step configuration container. The collected configuration is applied upon invocation.
 */
public interface Step
extends Runnable {

	/**
	 Configures the scenario step
	 @param stepConfig a dictionary of the configuration values to override the inherited config
	 @return <b>new</b> step with the applied config values
	 */
	Step config(final Map<String, Object> stepConfig);

	/**
	 * Notify the step that it's child of the specified parent step
	 * @param parentStep the step invoked the method
	 * @return <b>new</b> step with applied parent step
	 */
	Step parent(final CompositeStep parentStep);

	/**
	 * The step's config
	 * @return config instance
	 */
	Map<String, Object> getStepConfig();
}
