package com.emc.mongoose.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface InstallHook {

	String APP_NAME = "mongoose";
	String CONFIG_DIR = "config";
	String DEFAULT_CONFIG_FILE_NAME = "defaults.json";
	String CONFIG_SCHEMA_FILE_NAME = "config-schema.json";
	String FILE_SEP = System.getProperty("file.separator");
	String LINE_SEP = System.getProperty("line.separator");
	String USER_HOME = System.getProperty("user.home");

	static void run() {

		final URL defaultConfigUrl = Config.class
			.getClassLoader().getResource(DEFAULT_CONFIG_FILE_NAME);
		if(defaultConfigUrl == null) {
			System.err.println("No bundled default config found");
			return;
		}
		final Config defaultConfig;
		try {
			defaultConfig = Config.loadFromResource(defaultConfigUrl);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
			return;
		}
		final String appVersion = defaultConfig.getVersion();

		System.out.println(APP_NAME + " version " + appVersion);

		final Path appHomePath = Paths.get(USER_HOME, "." + APP_NAME, appVersion);
		try {
			Files.createDirectories(appHomePath);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
			return;
		}
		try(
			final InputStream defaultConfigInput = Config.class.getResourceAsStream(
				DEFAULT_CONFIG_FILE_NAME
			)
		) {
			final long copiedBytesCount = Files.copy(
				defaultConfigInput,
				Paths.get(appHomePath.toString(), CONFIG_DIR, DEFAULT_CONFIG_FILE_NAME)
			);
		} catch(final IOException e) {

		}
	}
}
