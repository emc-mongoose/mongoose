package com.emc.mongoose.run.scenario.jsr223;

import javax.script.Bindings;

/**
 A runnable step configuration container. The collected configuration is applied upon invocation.
 */
public interface Step
extends Runnable {

	/**
	 Configures the scenario step
	 @param stepConfig a dictionary of the configuration values to override the inherited config
	 @return <b>new</b> step builder with the applied config values
	 */
	Step config(final Bindings stepConfig);
}
