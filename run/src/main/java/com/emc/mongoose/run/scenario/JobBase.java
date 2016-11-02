package com.emc.mongoose.run.scenario;

import com.emc.mongoose.ui.config.Config;

/**
 Created by kurila on 08.04.16.
 */

public abstract class JobBase
implements Job {

	protected final Config localConfig;

	protected JobBase(final Config appConfig) {
		localConfig = new Config(appConfig);
	}

	@Override
	public final Config getConfig() {
		return localConfig;
	}
}
