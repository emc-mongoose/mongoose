package com.emc.mongoose.ui.config.reader.jackson;

import com.emc.mongoose.common.exception.IoFireball;
import com.emc.mongoose.ui.config.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.WordUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;

import static com.emc.mongoose.common.Constants.FNAME_CONFIG;

/**
 Created by kurila on 14.07.16.
 */
public abstract class ConfigLoader {

	private ConfigLoader() {}

	public static Config loadDefaultConfig()
	throws IoFireball {
		
		final URL configUrl = ConfigLoader.class.getClassLoader().getResource(FNAME_CONFIG);
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
		
		Object configNode;
		
		for(final String propName : System.getProperties().stringPropertyNames()) {
			
			final String propValue = System.getProperty(propName);
			final String propNameParts[] = propName.split("\\.");
			configNode = config;
			
			try {
				for(final String propNamePart : propNameParts) {
					final Method methods[] = config.getClass().getMethods();
					for(final Method m : methods) {
						System.out.println(m.getName());
						if(
							m.getName().equals(
								"get" + WordUtils.capitalize(propNamePart) + "Config"
							)
						) {
							configNode = m.invoke(configNode);
						} else if(
							m.getName().equals("set" + WordUtils.capitalize(propNamePart))
						) {
							System.out.println(m.getParameters()[0]);
						} else {
							break;
						}
					}
				}
			} catch(final Throwable e) {
				e.printStackTrace(System.out);
			}
		}
		
		return config;
	}
}
