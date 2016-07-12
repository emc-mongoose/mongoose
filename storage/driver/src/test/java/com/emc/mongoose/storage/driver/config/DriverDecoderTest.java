package com.emc.mongoose.storage.driver.config;

import com.emc.mongoose.common.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 Created on 11.07.16.
 */
public class DriverDecoderTest {

	private static String parameterErrorMessage(final String content) {
		return "Wrong " + content + " parameter";
	}

	@Test
	public void shouldCreateConfig() throws Exception{
		final DriverDecoder driverDecoder = new DriverDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final DriverConfig driverConfig =
			driverDecoder.decode(defaults);
		assertEquals(parameterErrorMessage("load.concurrency"),
			driverConfig.getLoadConfig().getConcurrency(), 1);
	}
}
