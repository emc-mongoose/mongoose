import com.emc.mongoose.config.GeneratorConfig;
import com.emc.mongoose.config.GeneratorDecoder;
import com.emc.mongoose.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 Created on 11.07.16.
 */
public class GeneratorDecoderTest {

	private static String parameterErrorMessage(final String content) {
		return "Wrong " + content + " parameter";
	}

	@Test
	public void shouldCreateConfig() throws Exception{
		final GeneratorDecoder generatorDecoder = new GeneratorDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final GeneratorConfig generatorConfig =
			generatorDecoder.decode(defaults);
		final GeneratorConfig.Item item = generatorConfig.item();
		assertEquals(parameterErrorMessage("item.type"), item.getType(), "data");
		final GeneratorConfig.Item.Data data = item.data();
		final GeneratorConfig.Item.Data.Content content = data.content();
		assertNull(parameterErrorMessage("item.data.content.file"), content.getFile());
		assertEquals(parameterErrorMessage("item.data.content.seed"), content.getSeed(), "7a42d9c483244167");
		assertEquals(parameterErrorMessage("item.data.content.ringSize"), content.getRingSize(), "4MB");
		assertEquals(parameterErrorMessage("item.data.ranges"), data.getRanges(), 0);
		assertEquals(parameterErrorMessage("item.data.size"), data.getSize(), "1MB");
		assertEquals(parameterErrorMessage("item.data.verify"), data.getVerify(), true);
		final GeneratorConfig.Item.Destination destination = item.destination();
		assertNull(parameterErrorMessage("item.dst.container"), destination.getContainer());
		assertNull(parameterErrorMessage("item.dst.file"), destination.getFile());
		final GeneratorConfig.Item.Source source = item.source();
		assertNull(parameterErrorMessage("item.src.container"), source.getContainer());
		assertNull(parameterErrorMessage("item.src.file"), source.getFile());
		assertEquals(parameterErrorMessage("item.src.batchSize"), source.getBatchSize(), 4096);
		final GeneratorConfig.Item.Naming naming = item.naming();
		assertEquals(parameterErrorMessage("naming.type"), naming.getType(), "random");
		assertNull(parameterErrorMessage("naming.prefix"), naming.getPrefix());
		assertEquals(parameterErrorMessage("naming.radix"), naming.getRadix(), 36);
		assertEquals(parameterErrorMessage("naming.offset"), naming.getOffset(), 0);
		assertEquals(parameterErrorMessage("naming.length"), naming.getLength(), 13);
	}

}
