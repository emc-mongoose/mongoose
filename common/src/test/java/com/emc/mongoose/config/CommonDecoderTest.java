package com.emc.mongoose.config;

import com.emc.mongoose.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 Created on 11.07.16.
 */
public class CommonDecoderTest {

	@Test
	public void shouldCreateConfig() throws Exception{
		final CommonDecoder commonDecoder = new CommonDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final CommonConfig commonConfig =
			commonDecoder.decode(defaults);
		assertEquals("Decoding was failed",
			commonConfig.network().socket().getTimeoutMilliSec(), 1000000);
	}

}
