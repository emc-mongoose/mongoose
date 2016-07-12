package com.emc.mongoose.monitor.config;

import com.emc.mongoose.common.config.decoder.DecodeException;
import com.emc.mongoose.common.config.decoder.Decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public class MonitorDecoder implements Decoder<MonitorConfig> {
	
	@Override
	public MonitorConfig decode(final JsonObject monitorJson)
	throws DecodeException {
		final JsonObject jobJson = getJsonObject(monitorJson, MonitorConfig.KEY_JOB);
		final boolean circular = jobJson.getBoolean(MonitorConfig.JobConfig.KEY_CIRCULAR);
		final String type = getString(jobJson, MonitorConfig.JobConfig.KEY_TYPE);
		final JsonObject limitJson = getJsonObject(jobJson, MonitorConfig.JobConfig.KEY_LIMIT);
		final MonitorConfig.JobConfig.LimitConfig
			limitConfig = new MonitorConfig.JobConfig.LimitConfig(
			limitJson.getInt(MonitorConfig.JobConfig.LimitConfig.KEY_COUNT),
			limitJson.getInt(MonitorConfig.JobConfig.LimitConfig.KEY_RATE),
			limitJson.getInt(MonitorConfig.JobConfig.LimitConfig.KEY_SIZE),
			getString(limitJson, MonitorConfig.JobConfig.LimitConfig.KEY_TIME)
		);
		final MonitorConfig.JobConfig jobConfig = new MonitorConfig.JobConfig(circular, type, limitConfig);
		final JsonObject metricsJson = getJsonObject(monitorJson, MonitorConfig.KEY_METRICS);
		final MonitorConfig.MetricsConfig metricsConfig = new MonitorConfig.MetricsConfig(
			metricsJson.getBoolean(MonitorConfig.MetricsConfig.KEY_INTERMEDIATE),
			getString(metricsJson, MonitorConfig.MetricsConfig.KEY_PERIOD),
			metricsJson.getBoolean(MonitorConfig.MetricsConfig.KEY_PRECONDITION)
		);
		final JsonObject runJson = getJsonObject(monitorJson, MonitorConfig.KEY_RUN);
		final MonitorConfig.RunConfig runConfig = new MonitorConfig.RunConfig(
			getString(runJson, MonitorConfig.RunConfig.KEY_FILE, null),
			getString(runJson, MonitorConfig.RunConfig.KEY_ID, null)
		);
		return new MonitorConfig(jobConfig, metricsConfig, runConfig);
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
