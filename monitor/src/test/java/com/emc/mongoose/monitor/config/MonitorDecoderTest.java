package com.emc.mongoose.monitor.config;

import com.emc.mongoose.common.config.decoder.Decoder;
import com.emc.mongoose.common.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 Created on 11.07.16.
 */
public class MonitorDecoderTest {

	private static String parameterErrorMessage(final String content) {
		return "Wrong " + content + " parameter";
	}

	@Test
	public void shouldCreateConfig() throws Exception{
		final MonitorConfig monitorConfig = ConfigReader.loadConfig(new MonitorDecoder());
		assertNotNull(monitorConfig);
		final MonitorConfig.JobConfig jobConfig = monitorConfig.getJobConfig();
		assertEquals(parameterErrorMessage("job.circular"), jobConfig.getCircular(), false);
		final MonitorConfig.JobConfig.LimitConfig limitConfig = jobConfig.getLimitConfig();
		assertEquals(parameterErrorMessage("job.limit.count"), limitConfig.getCount(), 0);
		assertEquals(parameterErrorMessage("job.limit.rate"), limitConfig.getRate(), 0);
		assertEquals(parameterErrorMessage("job.limit.size"), limitConfig.getSize(), 0);
		assertEquals(parameterErrorMessage("job.limit.time"), limitConfig.getTime(), "0s");
		final MonitorConfig.MetricsConfig metricsConfig = monitorConfig.getMetricsConfig();
		assertEquals(parameterErrorMessage("metrics.intermediate"), metricsConfig.getIntermediate(), false);
		assertEquals(parameterErrorMessage("metrics.period"), metricsConfig.getPeriod(), "10s");
		assertEquals(parameterErrorMessage("metrics.precondition"), metricsConfig.getPrecondition(), false);
		final MonitorConfig.RunConfig runConfig = monitorConfig.getRunConfig();
		assertNull(parameterErrorMessage("run.file"), runConfig.getFile());
		assertNull(parameterErrorMessage("run.id"), runConfig.getId());
	}

}
