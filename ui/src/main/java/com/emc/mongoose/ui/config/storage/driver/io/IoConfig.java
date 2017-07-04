package com.emc.mongoose.ui.config.storage.driver.io;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class IoConfig
implements Serializable {

	public static final String KEY_WORKERS = "workers";

	public final void setWorkers(final int workers) {
		this.workers = workers;
	}

	@JsonProperty(KEY_WORKERS) private int workers;

	public final int getWorkers() {
		return workers;
	}

	public IoConfig() {
	}

	public IoConfig(final IoConfig other) {
		this.workers = other.getWorkers();
	}
}