package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.conf.AppConfig;
/**
 Created by kurila on 08.04.16.
 */
public abstract class JobContainerBase
implements JobContainer {

	protected final AppConfig localConfig;
	protected final long limitTime;

	protected JobContainerBase(final AppConfig appConfig) {
		localConfig = appConfig;
		limitTime = localConfig.getLoadLimitTime();
	}

	protected JobContainerBase(final AppConfig appConfig, long limitTime) {
		localConfig = appConfig;
		this.limitTime = limitTime;
	}

	@Override
	public final AppConfig getConfig() {
		return localConfig;
	}
}
