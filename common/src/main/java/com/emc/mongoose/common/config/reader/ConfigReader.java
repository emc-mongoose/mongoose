package com.emc.mongoose.common.config.reader;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 Created on 11.07.16.
 */
public class ConfigReader {

	public static JsonObject readJson(final String jsonFilePath) {
		final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		final InputStream fileAsInputStream = classloader.getResourceAsStream(jsonFilePath);
		return Json.createReader(new InputStreamReader(fileAsInputStream)).readObject();
	}

}
