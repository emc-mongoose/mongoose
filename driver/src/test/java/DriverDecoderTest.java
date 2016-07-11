import com.emc.mongoose.config.DriverConfig;
import com.emc.mongoose.config.DriverDecoder;
import com.emc.mongoose.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 Created on 11.07.16.
 */
public class DriverDecoderTest {

	@Test
	public void shouldCreateConfig() throws Exception{
		final DriverDecoder driverDecoder = new DriverDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final DriverConfig driverConfig =
			driverDecoder.decode(defaults);
		assertEquals("Decoding was failed", driverConfig.load().getConcurrency(), 1);
	}
}
