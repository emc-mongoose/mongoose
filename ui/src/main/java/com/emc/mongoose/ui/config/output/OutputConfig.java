package com.emc.mongoose.ui.config.output;

import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class OutputConfig
implements Serializable {

	public static final String KEY_COLOR = "color";
	public static final String KEY_METRICS = "metrics";

	public final void setColor(final boolean colorFlag) {
		this.colorFlag = colorFlag;
	}

	public final void setMetricsConfig(final MetricsConfig metricsConfig) {
		this.metricsConfig = metricsConfig;
	}

	@JsonProperty(KEY_COLOR) private boolean colorFlag;
	@JsonProperty(KEY_METRICS) private MetricsConfig metricsConfig;

	public OutputConfig() {
	}

	public OutputConfig(final OutputConfig other) {
		this.colorFlag = other.getColor();
		this.metricsConfig = new MetricsConfig(other.getMetricsConfig());
	}

	public final boolean getColor() {
		return colorFlag;
	}

	public final MetricsConfig getMetricsConfig() {
		return metricsConfig;
	}
}