package com.emc.mongoose.ui.config.load.service;

import com.emc.mongoose.api.model.concurrent.DaemonBase;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 26.07.17.
 */
public final class ServiceConfig
implements Serializable {

	public static final String KEY_THREADS = "threads";

	public final void setThreads(final int count) {
		this.threads = count;
		//Loggers.MSG.info("Set the service threads count to {}", count == 0 ? "<AUTO>" : count);
		DaemonBase.setThreadCount(count);
	}

	@JsonProperty(KEY_THREADS) private int threads;

	public ServiceConfig() {
	}

	public ServiceConfig(final ServiceConfig other) {
		this.threads = other.getThreads();
	}

	public final int getThreads() {
		return threads;
	}
}
