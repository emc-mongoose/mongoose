package com.emc.mongoose.common.config.reader.jsr353;

import com.emc.mongoose.common.config.Constants;
import com.emc.mongoose.common.config.reader.jsr353.decoder.DecodeException;
import com.emc.mongoose.common.config.reader.jsr353.decoder.Decoder;
import com.emc.mongoose.common.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import javax.json.Json;
//import javax.json.JsonObject;
//import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 Created on 11.07.16.
 */
public class Jsr353ConfigReader {

	private final static Logger LOG = LogManager.getLogger();


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
