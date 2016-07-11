import com.emc.mongoose.config.StorageMockConfig;
import com.emc.mongoose.config.StorageMockDecoder;
import com.emc.mongoose.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 Created on 11.07.16.
 */
public class StorageMockDecoderTest {

	@Test
	public void shouldCreateConfig() throws Exception{
		final StorageMockDecoder storageMockDecoder = new StorageMockDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final StorageMockConfig storageMockConfig =
			storageMockDecoder.decode(defaults);
		assertEquals("Decoding was failed", storageMockConfig.getHeadCount(), 1);
	}

}
