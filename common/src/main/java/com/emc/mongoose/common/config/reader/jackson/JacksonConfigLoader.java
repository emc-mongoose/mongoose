package com.emc.mongoose.common.config.reader.jackson;

import com.emc.mongoose.common.config.Config;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static com.emc.mongoose.common.config.Constants.FNAME_CONFIG;

/**
 Created by kurila on 14.07.16.
 */
public abstract class JacksonConfigLoader {

	private JacksonConfigLoader() {}

	public static Config loadDefaultConfig()
	throws IOException {
		final URL configUrl = JacksonConfigLoader.class.getClassLoader().getResource(FNAME_CONFIG);
		if(configUrl == null) {
			return null;
		}
		final ObjectMapper mapper = new ObjectMapper();
		Config config = null;
		try(final InputStream is = configUrl.openStream()) {
			config = mapper.readValue(is, Config.class);
		}
		return config;
	}
}
