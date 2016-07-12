package com.emc.mongoose.generator.config;

import com.emc.mongoose.common.config.reader.ConfigReader;
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
		final GeneratorConfig.ItemConfig itemConfig = generatorConfig.item();
		assertEquals(parameterErrorMessage("item.type"), itemConfig.getType(), "data");
		final GeneratorConfig.ItemConfig.DataConfig dataConfig = itemConfig.getDataConfig();
		final GeneratorConfig.ItemConfig.DataConfig.ContentConfig contentConfig = dataConfig.getContentConfig();
		assertNull(parameterErrorMessage("item.data.content.file"), contentConfig.getFile());
		assertEquals(parameterErrorMessage("item.data.content.seed"), contentConfig.getSeed(), "7a42d9c483244167");
		assertEquals(parameterErrorMessage("item.data.content.ringSize"), contentConfig.getRingSize(), "4MB");
		assertEquals(parameterErrorMessage("item.data.ranges"), dataConfig.getRanges(), 0);
		assertEquals(parameterErrorMessage("item.data.size"), dataConfig.getSize(), "1MB");
		assertEquals(parameterErrorMessage("item.data.verify"), dataConfig.getVerify(), true);
		final GeneratorConfig.ItemConfig.DestinationConfig destinationConfig = itemConfig.getDestinationConfig();
		assertNull(parameterErrorMessage("item.dst.container"), destinationConfig.getContainer());
		assertNull(parameterErrorMessage("item.dst.file"), destinationConfig.getFile());
		final GeneratorConfig.ItemConfig.SourceConfig sourceConfig = itemConfig.getSourceConfig();
		assertNull(parameterErrorMessage("item.src.container"), sourceConfig.getContainer());
		assertNull(parameterErrorMessage("item.src.file"), sourceConfig.getFile());
		assertEquals(parameterErrorMessage("item.src.batchSize"), sourceConfig.getBatchSize(), 4096);
		final GeneratorConfig.ItemConfig.NamingConfig namingConfig = itemConfig.getNamingConfig();
		assertEquals(parameterErrorMessage("naming.type"), namingConfig.getType(), "random");
		assertNull(parameterErrorMessage("naming.prefix"), namingConfig.getPrefix());
		assertEquals(parameterErrorMessage("naming.radix"), namingConfig.getRadix(), 36);
		assertEquals(parameterErrorMessage("naming.offset"), namingConfig.getOffset(), 0);
		assertEquals(parameterErrorMessage("naming.length"), namingConfig.getLength(), 13);
	}

}
