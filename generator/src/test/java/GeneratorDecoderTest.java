import com.emc.mongoose.config.GeneratorConfig;
import com.emc.mongoose.config.GeneratorDecoder;
import com.emc.mongoose.config.reader.ConfigReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 Created on 11.07.16.
 */
public class GeneratorDecoderTest {

	@Test
	public void shouldCreateConfig() throws Exception{
		final GeneratorDecoder generatorDecoder = new GeneratorDecoder();
		final GeneratorConfig generatorConfig =
			generatorDecoder.decode(ConfigReader.readJson("defaults.json"));
		assertEquals(generatorConfig.item().getType(), "data");
	}

}
