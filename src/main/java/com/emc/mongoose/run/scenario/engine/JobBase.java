package com.emc.mongoose.run.scenario.engine;

import com.emc.mongoose.common.conf.AppConfig;

import java.io.IOException;
/**
 Created by kurila on 08.04.16.
 */
public abstract class JobBase
implements Job {

	protected final AppConfig localConfig;

	protected JobBase(final AppConfig appConfig) {
		try {
			localConfig = (AppConfig) appConfig.clone();
		} catch(final CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public final AppConfig getConfig() {
		return localConfig;
	}
}
