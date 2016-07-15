package com.emc.mongoose.ui.config.reader.jackson;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.common.exception.IoFireball;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static com.emc.mongoose.common.Constants.FNAME_CONFIG;

/**
 Created by kurila on 14.07.16.
 */
public abstract class JacksonConfigLoader {

	private JacksonConfigLoader() {}

	public static Config loadDefaultConfig()
	throws IoFireball {
		final URL configUrl = JacksonConfigLoader.class.getClassLoader().getResource(FNAME_CONFIG);
		if(configUrl == null) {
			return null;
		}
		final ObjectMapper mapper = new ObjectMapper();
		final Config config;
		try(final InputStream is = configUrl.openStream()) {
			config = mapper.readValue(is, Config.class);
		} catch(final IOException e) {
			throw new IoFireball(e);
		}
		return config;
	}
}
