package com.emc.mongoose.storage.driver.config;

import com.emc.mongoose.common.config.decoder.Decoder;
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
		final DriverConfig driverConfig = ConfigReader.loadConfig(new DriverDecoder());
		assertNotNull(driverConfig);
		assertEquals(parameterErrorMessage("load.concurrency"),
			driverConfig.getLoadConfig().getConcurrency(), 1);
	}
}
