package com.emc.mongoose.ui.config.reader.jackson;

import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
import com.emc.mongoose.ui.config.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.Constants.DIR_CONFIG;
import static com.emc.mongoose.common.Constants.FNAME_CONFIG;

/**
 Created by kurila on 14.07.16.
 */
public abstract class ConfigParser {

	private ConfigParser() {}

	public static Config loadDefaultConfig()
	throws IOException {
		final String defaultConfigPath = PathUtil.getBaseDir() + File.separator + DIR_CONFIG +
			File.separator + FNAME_CONFIG;
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(new File(defaultConfigPath), Config.class);
	}

	public static Config replace(
		final Config config, final String replacePattern, final Object newValue
	) throws OmgLookAtMyConsoleException, OmgDoesNotPerformException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		try {
			final String configText = mapper.writeValueAsString(config);
			final String newConfigText;
			final String rp = Pattern.quote(replacePattern);
			if(newValue == null) {
				newConfigText = configText.replaceAll("\"" + rp + "\"", "null");
			} else if(newValue instanceof Boolean || newValue instanceof Number) {
				newConfigText = configText
					.replaceAll("\"" + rp + "\"", newValue.toString())
					.replaceAll(rp, newValue.toString());
			} else if(newValue instanceof List) {
				final List<Object> newValues = (List<Object>) newValue;
				final List<String> newStrValues = new ArrayList<>();
				// replace the values in the source list with their textual representations
				Object nextValue;
				for(int i = 0; i < newValues.size(); i ++) {
					nextValue = newValues.get(i);
					if(nextValue == null) {
						newStrValues.add("null");
					} else if(nextValue instanceof Boolean || nextValue instanceof Number) {
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
				newConfigText = configText.replace(rp, newValueStr);
			} else if(newValue instanceof String) {
				newConfigText = configText.replaceAll(rp, (String) newValue);
			} else {
				throw new OmgLookAtMyConsoleException(
					"Unexpected replacement value type: " + newValue.getClass().getName()
				);
			}
			return mapper.readValue(newConfigText, Config.class);
		} catch(final JsonProcessingException e) {
			throw new OmgDoesNotPerformException(e);
		}
	}
	
	public static Map<String, Object> replace(
		final Map<String, Object> config, final String replacePattern, final Object newValue
	) throws OmgLookAtMyConsoleException {
		final Map<String, Object> newConfig = new HashMap<>();
		final String rp = Pattern.quote(replacePattern);
		Object v;
		String valueStr;
		for(final String k : config.keySet()) {
			v = config.get(k);
			if(v instanceof String) {
				valueStr = (String) v;
				if(valueStr.equals("\"" + replacePattern + "\"")) {
					v = newValue;
				} else {
					if(newValue == null) {
						v = valueStr.replaceAll(rp, "");
					} else if(
						newValue instanceof Boolean || newValue instanceof Number ||
						newValue instanceof String
					) {
						v = valueStr.replaceAll(rp, newValue.toString());
					} else if(newValue instanceof List) {
						final StringJoiner sj = new StringJoiner(",");
						for(final Object newValueElement : (List) newValue) {
							sj.add(newValueElement == null ? "" : newValueElement.toString());
						}
						v = valueStr.replaceAll(rp, sj.toString());
					} else {
						throw new OmgLookAtMyConsoleException(
							"Unexpected replacement value type: " + newValue.getClass().getName()
						);
					}
				}
			} else if(v instanceof Map) {
				v = replace((Map<String, Object>) v, replacePattern, newValue);
			}
			newConfig.put(k, v);
		}
		return newConfig;
	}
}
