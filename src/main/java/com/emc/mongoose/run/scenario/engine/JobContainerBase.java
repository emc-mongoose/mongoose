package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.conf.AppConfig;
/**
 Created by kurila on 08.04.16.
 */
public abstract class JobContainerBase
implements JobContainer {

	protected final AppConfig localConfig;

	protected JobContainerBase(final AppConfig appConfig) {
		localConfig = appConfig;
	}

	@Override
	public final AppConfig getConfig() {
		return localConfig;
	}
}
