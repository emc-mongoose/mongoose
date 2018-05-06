package com.emc.mongoose;

import com.emc.mongoose.config.Config;
import com.github.akurilov.commons.system.SizeInBytes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.PATH_CONFIG_SCHEMA;
import static com.emc.mongoose.Constants.PATH_DEFAULTS;
import static com.emc.mongoose.Constants.USER_HOME;

public final class InstallHook
implements Runnable {

	private final Path appHomePath;

	public InstallHook() {
		final String appVersion = defaultVersion();
		System.out.println(APP_NAME + " v " + appVersion);
		appHomePath = Paths.get(USER_HOME, "." + APP_NAME, appVersion);
		try {
			Files.createDirectories(appHomePath);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
			return;
		}
	}

	private static String defaultVersion()
	throws IllegalStateException {
		final URL defaultConfigUrl = InstallHook.class.getClassLoader().getResource(PATH_DEFAULTS);
		if(defaultConfigUrl == null) {
			throw new IllegalStateException("No bundled default config found");
		}
		final Config defaultConfig;
		try {
			defaultConfig = Config.loadFromUrl(defaultConfigUrl);
		} catch(final IOException e) {
			throw new IllegalStateException(
				"Failed to load the default config from the resources", e
			);
		}
		return defaultConfig.getVersion();
	}

	public final void run() {
		installResourcesFile(appHomePath, PATH_DEFAULTS);
		installResourcesFile(appHomePath, PATH_CONFIG_SCHEMA);

		try(
			final BufferedReader reader = new BufferedReader(
				new InputStreamReader(
					InstallHook.class.getClassLoader().getResourceAsStream("example")
				)
			)
		) {
			String line;
			while(null != (line = reader.readLine())) {
				System.out.println(line);
			}
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
	}

	private static void installResourcesFile(final Path appHomePath, final String relPath) {
		final Path dstPath = Paths.get(appHomePath.toString(), relPath);
		System.out.print("Checking the file " + dstPath + "... ");
		if(dstPath.toFile().exists()) {
			System.out.println("exists, skipping");
			return;
		}
		dstPath.getParent().toFile().mkdirs();
		try(
			final InputStream
				srcFileInput = InstallHook.class.getResourceAsStream(File.separator + relPath)
		) {
			final long copiedBytesCount = Files.copy(srcFileInput, dstPath);
			System.out.println("installed (" + SizeInBytes.formatFixedSize(copiedBytesCount) + ")");
		} catch(final IOException e) {
			System.out.println("failed to install (" + e + ")");
		}
	}
}
