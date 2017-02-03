package com.emc.mongoose.ui.config.reader.jsr353;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import javax.json.Json;
//import javax.json.JsonObject;
//import javax.json.JsonReader;

/**
 Created on 11.07.16.
 */
public class Jsr353ConfigReader {

	private static final Logger LOG = LogManager.getLogger();


	/*private static JsonObject readJson(final ClassLoader classLoader, final String jsonFilePath) {
		try(final InputStream fileAsInputStream = classLoader.getResourceAsStream(jsonFilePath)) {
			return readJsonAsStream(fileAsInputStream);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to open the configuration file");
		}
		return null;
	}

	private static JsonObject readJsonAsStream(final InputStream jsonInputStream) {
		try(
			final JsonReader jsonReader = Json.createReader(
				new InputStreamReader(jsonInputStream)
			)
		) {
			return jsonReader.readObject();
		}
	}

	public static <T> T loadConfig(final Decoder<T> decoder) {
		try {
			final JsonObject jsonConfig = readJson(
				decoder.getClass().getClassLoader(), Constants.FNAME_CONFIG
			);
			return decoder.decode(jsonConfig);
		} catch(final DecodeException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to read the configuration file");
		}
		return null;
	}*/
}
