package com.emc.mongoose.ui.config.reader.jackson;

import com.emc.mongoose.common.exception.IoFireball;
import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.ui.config.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.emc.mongoose.common.Constants.FNAME_CONFIG;

/**
 Created by kurila on 14.07.16.
 */
public abstract class ConfigParser {

	private ConfigParser() {}

	public static Config loadDefaultConfig()
	throws IoFireball {
		
		final URL configUrl = ConfigParser.class.getClassLoader().getResource(FNAME_CONFIG);
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

	public static Config replace(
		final Config config, final String replacePattern, final Object newValue
	) throws OmgLookAtMyConsoleException, OmgDoesNotPerformException, IoFireball {
		final ObjectMapper mapper = new ObjectMapper();
		try {
			final String configText = mapper.writeValueAsString(config);
			final String newConfigText;
			if(newValue == null) {
				newConfigText = configText.replaceAll("\"" + replacePattern + "\"", "null");
			} else if(newValue instanceof Boolean) {
				newConfigText = configText
					.replaceAll("\"" + replacePattern + "\"", newValue.toString())
					.replaceAll(replacePattern, newValue.toString());
			} else if(newValue instanceof Long) {
				newConfigText = configText
					.replaceAll("\"" + replacePattern + "\"", newValue.toString())
					.replaceAll(replacePattern, newValue.toString());
			} else if(newValue instanceof Double) {
				newConfigText = configText
					.replaceAll("\"" + replacePattern + "\"", newValue.toString())
					.replaceAll(replacePattern, newValue.toString());
			} else if(newValue instanceof List) {
				final List<Object> newValues = (List<Object>) newValue;
				final List<String> newStrValues = new ArrayList<>();
				// replace the values in the source list with their textual representations
				Object nextValue;
				for(int i = 0; i < newValues.size(); i ++) {
					nextValue = newValues.get(i);
					if(nextValue == null) {
						newStrValues.add("null");
					} else if(nextValue instanceof Boolean) {
						newStrValues.add(nextValue.toString());
					} else if(nextValue instanceof Long) {
						newStrValues.add(nextValue.toString());
					} else if(nextValue instanceof Double) {
						newStrValues.add(nextValue.toString());
					} else if(nextValue instanceof String) {
						newStrValues.add("\"" + nextValue + "\"");
					} else {
						throw new OmgLookAtMyConsoleException(
							"Unexpected replacement value type: " + nextValue.getClass().getName()
						);
					}
				}
				final String newValueStr = "[" + String.join(",", newStrValues) + "]";
				newConfigText = configText.replace(replacePattern, newValueStr);
			} else if(newValue instanceof String) {
				newConfigText = configText.replaceAll(replacePattern, (String) newValue);
			} else {
				throw new OmgLookAtMyConsoleException(
					"Unexpected replacement value type: " + newValue.getClass().getName()
				);
			}
			return mapper.readValue(newConfigText, Config.class);
		} catch(final JsonProcessingException e) {
			throw new OmgDoesNotPerformException(e);
		} catch(final IOException e) {
			throw new IoFireball(e);
		}
	}
}
