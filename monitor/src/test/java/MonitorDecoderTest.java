import com.emc.mongoose.config.MonitorConfig;
import com.emc.mongoose.config.MonitorDecoder;
import com.emc.mongoose.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 Created on 11.07.16.
 */
public class MonitorDecoderTest {

	@Test
	public void shouldCreateConfig() throws Exception{
		final MonitorDecoder monitorDecoder = new MonitorDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final MonitorConfig monitorConfig =
			monitorDecoder.decode(defaults);
		assertEquals("Decoding was failed", monitorConfig.job().getCircular(), false);
	}

}
