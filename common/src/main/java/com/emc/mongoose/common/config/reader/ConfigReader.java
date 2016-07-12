package com.emc.mongoose.common.config.reader;

import com.emc.mongoose.common.config.decoder.DecodeException;
import com.emc.mongoose.common.config.decoder.Decoder;
import com.emc.mongoose.common.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 Created on 11.07.16.
 */
public class ConfigReader{

	private final static Logger LOG = LogManager.getLogger();
	private ClassLoader classLoader;

	private ConfigReader(final Class aClass) {
		classLoader = aClass.getClassLoader();
	}

	private JsonObject readJson(final String jsonFilePath) {
		try(final InputStream fileAsInputStream = classLoader.getResourceAsStream(jsonFilePath)) {
			try(
				final JsonReader jsonReader = Json.createReader(
					new InputStreamReader(fileAsInputStream))
			) {
				return jsonReader.readObject();
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to open the configuration file");
		}
		return null;
	}

	public static <T> T loadConfig(final Decoder<T> decoder) {
		try {
			return decoder.decode(new ConfigReader(decoder.getClass()).readJson("defaults.json"));
		} catch(final DecodeException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to read the configuration file");
		}
		return null;
	}
}
