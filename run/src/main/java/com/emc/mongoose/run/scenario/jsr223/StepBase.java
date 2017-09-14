package com.emc.mongoose.run.scenario.jsr223;

import com.emc.mongoose.ui.config.Config;

/**
 The scenario step base containing the immutable configuration.
 */
public abstract class StepBase
implements Step {

	protected final Config config;

	protected StepBase(final Config config) {
		this.config = config;
	}
}
