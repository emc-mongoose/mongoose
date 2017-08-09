package com.emc.mongoose.ui.config.load.generator.recycle;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 25.07.17.
 */
public final class RecycleConfig
implements Serializable {

	public static final String KEY_ENABLED = "enabled";
	public static final String KEY_LIMIT = "limit";

	public final void setEnabled(final boolean enabledFlag) {
		this.enabledFlag = enabledFlag;
	}

	public final void setLimit(final int limit) {
		this.limit = limit;
	}

	@JsonProperty(KEY_ENABLED) private boolean enabledFlag;
	@JsonProperty(KEY_LIMIT) private int limit;

	public RecycleConfig() {
	}

	public RecycleConfig(final RecycleConfig other) {
		this.enabledFlag = other.getEnabled();
		this.limit = other.getLimit();
	}

	public final boolean getEnabled() {
		return enabledFlag;
	}

	public final int getLimit() {
		return limit;
	}
}
