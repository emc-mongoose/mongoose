import com.emc.mongoose.monitor.config.MonitorConfig;
import com.emc.mongoose.monitor.config.MonitorDecoder;
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
		final MonitorDecoder monitorDecoder = new MonitorDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final MonitorConfig monitorConfig =
			monitorDecoder.decode(defaults);
		final MonitorConfig.Job job = monitorConfig.job();
		assertEquals(parameterErrorMessage("job.circular"), job.getCircular(), false);
		final MonitorConfig.Job.Limit limit = job.limit();
		assertEquals(parameterErrorMessage("job.limit.count"), limit.getCount(), 0);
		assertEquals(parameterErrorMessage("job.limit.rate"), limit.getRate(), 0);
		assertEquals(parameterErrorMessage("job.limit.size"), limit.getSize(), 0);
		assertEquals(parameterErrorMessage("job.limit.time"), limit.getTime(), "0s");
		final MonitorConfig.Metrics metrics = monitorConfig.metrics();
		assertEquals(parameterErrorMessage("metrics.intermediate"), metrics.getIntermediate(), false);
		assertEquals(parameterErrorMessage("metrics.period"), metrics.getPeriod(), "10s");
		assertEquals(parameterErrorMessage("metrics.precondition"), metrics.getPrecondition(), false);
		final MonitorConfig.Run run = monitorConfig.run();
		assertNull(parameterErrorMessage("run.file"), run.getFile());
		assertNull(parameterErrorMessage("run.id"), run.getId());
	}

}
