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
		final JsonObject jobJson = monitorJson.getJsonObject(MonitorConfig.KEY_JOB);
		final boolean circular = jobJson.getBoolean(MonitorConfig.Job.KEY_CIRCULAR);
		final JsonObject limitJson = jobJson.getJsonObject(MonitorConfig.Job.KEY_LIMIT);
		final MonitorConfig.Job.Limit limit = new MonitorConfig.Job.Limit(
			limitJson.getInt(MonitorConfig.Job.Limit.KEY_COUNT),
			limitJson.getInt(MonitorConfig.Job.Limit.KEY_RATE),
			limitJson.getInt(MonitorConfig.Job.Limit.KEY_SIZE),
			limitJson.getString(MonitorConfig.Job.Limit.KEY_TIME)
		);
		final MonitorConfig.Job job = new MonitorConfig.Job(circular, limit);
		final JsonObject metricsJson = monitorJson.getJsonObject(MonitorConfig.KEY_METRICS);
		final MonitorConfig.Metrics metrics = new MonitorConfig.Metrics(
			metricsJson.getBoolean(MonitorConfig.Metrics.KEY_INTERMEDIATE),
			metricsJson.getString(MonitorConfig.Metrics.KEY_PERIOD),
			metricsJson.getBoolean(MonitorConfig.Metrics.KEY_PRECONDITION)
		);
		final JsonObject runJson = monitorJson.getJsonObject(MonitorConfig.KEY_RUN);
		final MonitorConfig.Run run = new MonitorConfig.Run(
			runJson.getString(MonitorConfig.Run.KEY_FILE, null),
			runJson.getString(MonitorConfig.Run.KEY_ID, null)
		);
		return new MonitorConfig(job, metrics, run);
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
