package com.emc.mongoose.ui.config.output.metrics.average.table.header;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class HeaderConfig
implements Serializable {

	public static final String KEY_PERIOD = "period";

	public final void setPeriod(final int period) {
		this.period = period;
	}

	@JsonProperty(KEY_PERIOD) private int period;

	public HeaderConfig() {
	}

	public HeaderConfig(final HeaderConfig other) {
		this.period = other.getPeriod();
	}

	public final int getPeriod() {
		return period;
	}
}