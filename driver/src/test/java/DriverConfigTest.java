import com.emc.mongoose.config.DriverConfig;
import com.emc.mongoose.config.DriverDecoder;
import com.emc.mongoose.config.reader.ConfigReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 Created on 11.07.16.
 */
public class DriverConfigTest {

	@Test
	public void shouldCreateConfig() throws Exception{
		final DriverDecoder driverDecoder = new DriverDecoder();
		final DriverConfig driverConfig =
			driverDecoder.decode(ConfigReader.readJson("defaults.json"));
		assertEquals(driverConfig.load().getConcurrency(), 1);
	}
}
