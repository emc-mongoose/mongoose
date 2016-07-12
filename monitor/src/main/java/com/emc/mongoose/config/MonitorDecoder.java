package com.emc.mongoose.config;

import com.emc.mongoose.config.decoder.DecodeException;
import com.emc.mongoose.config.decoder.Decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public class MonitorDecoder implements Decoder<MonitorConfig> {
	
	@Override
	public MonitorConfig decode(final JsonObject monitorJson)
	throws DecodeException {
		final JsonObject jobJson = monitorJson.getJsonObject(MonitorConfig.KEY_JOB);
		final boolean circular = jobJson.getBoolean(MonitorConfig.JobConfig.KEY_CIRCULAR);
		final JsonObject limitJson = jobJson.getJsonObject(MonitorConfig.JobConfig.KEY_LIMIT);
		final MonitorConfig.JobConfig.LimitConfig
			limitConfig = new MonitorConfig.JobConfig.LimitConfig(
			limitJson.getInt(MonitorConfig.JobConfig.LimitConfig.KEY_COUNT),
			limitJson.getInt(MonitorConfig.JobConfig.LimitConfig.KEY_RATE),
			limitJson.getInt(MonitorConfig.JobConfig.LimitConfig.KEY_SIZE),
			limitJson.getString(MonitorConfig.JobConfig.LimitConfig.KEY_TIME)
		);
		final MonitorConfig.JobConfig jobConfig = new MonitorConfig.JobConfig(circular, limitConfig);
		final JsonObject metricsJson = monitorJson.getJsonObject(MonitorConfig.KEY_METRICS);
		final MonitorConfig.MetricsConfig metricsConfig = new MonitorConfig.MetricsConfig(
			metricsJson.getBoolean(MonitorConfig.MetricsConfig.KEY_INTERMEDIATE),
			metricsJson.getString(MonitorConfig.MetricsConfig.KEY_PERIOD),
			metricsJson.getBoolean(MonitorConfig.MetricsConfig.KEY_PRECONDITION)
		);
		final JsonObject runJson = monitorJson.getJsonObject(MonitorConfig.KEY_RUN);
		final MonitorConfig.RunConfig runConfig = new MonitorConfig.RunConfig(
			runJson.getString(MonitorConfig.RunConfig.KEY_FILE, null),
			runJson.getString(MonitorConfig.RunConfig.KEY_ID, null)
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
