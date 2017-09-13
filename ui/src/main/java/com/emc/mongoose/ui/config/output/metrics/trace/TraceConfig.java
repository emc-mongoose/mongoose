package com.emc.mongoose.ui.config.output.metrics.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class TraceConfig
implements Serializable {

	public static final String KEY_PERSIST = "persist";

	public final void setPersist(final boolean persistFlag) {
		this.persistFlag = persistFlag;
	}

	@JsonProperty(KEY_PERSIST) private boolean persistFlag;

	public TraceConfig() {
	}

	public TraceConfig(final TraceConfig other) {
		this.persistFlag = other.getPersist();
	}

	public final boolean getPersist() {
		return persistFlag;
	}
}