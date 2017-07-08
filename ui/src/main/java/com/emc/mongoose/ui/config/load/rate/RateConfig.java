package com.emc.mongoose.ui.config.load.rate;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 08.07.17.
 */
public final class RateConfig
implements Serializable {

	public static final String KEY_LIMIT = "limit";

	public final void setLimit(final double limit) {
		this.limit = limit;
	}

	@JsonProperty(KEY_LIMIT) private double limit;

	public RateConfig() {
	}

	public RateConfig(final RateConfig other) {
		this.limit = other.getLimit();
	}

	public final double getLimit() {
		return limit;
	}
}
