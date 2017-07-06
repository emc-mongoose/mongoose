package com.emc.mongoose.ui.config.output.metrics;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.output.metrics.service.ServiceConfig;
import com.emc.mongoose.ui.config.output.metrics.summary.SummaryConfig;
import com.emc.mongoose.ui.config.output.metrics.trace.TraceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class MetricsConfig
implements Serializable {

	public static final String KEY_AVERAGE = "average";
	public static final String KEY_SUMMARY = "summary";
	public static final String KEY_TRACE = "trace";
	public static final String KEY_THRESHOLD = "threshold";

	public final void setAverageConfig(final AverageConfig averageConfig) {
		this.averageConfig = averageConfig;
	}

	public final void setSummaryConfig(final SummaryConfig summaryConfig) {
		this.summaryConfig = summaryConfig;
	}

	public final void setTraceConfig(final TraceConfig traceConfig) {
		this.traceConfig = traceConfig;
	}

	public final void setThreshold(final double threshold) {
		this.threshold = threshold;
	}

	@JsonProperty(KEY_AVERAGE) private AverageConfig averageConfig;
	@JsonProperty(KEY_SUMMARY) private SummaryConfig summaryConfig;
	@JsonProperty(KEY_TRACE) private TraceConfig traceConfig;
	@JsonProperty(KEY_THRESHOLD) private double threshold;

	public MetricsConfig() {
	}

	public MetricsConfig(final MetricsConfig other) {
		this.averageConfig = new AverageConfig(other.getAverageConfig());
		this.summaryConfig = new SummaryConfig(other.getSummaryConfig());
		this.traceConfig = new TraceConfig(other.getTraceConfig());
		this.threshold = other.getThreshold();
	}

	public final AverageConfig getAverageConfig() {
		return averageConfig;
	}

	public final SummaryConfig getSummaryConfig() {
		return summaryConfig;
	}

	public final TraceConfig getTraceConfig() {
		return traceConfig;
	}

	public final double getThreshold() {
		return threshold;
	}
}