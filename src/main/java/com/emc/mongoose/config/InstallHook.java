package com.emc.mongoose.config;

import com.emc.mongoose.model.Constants;
import com.github.akurilov.commons.system.SizeInBytes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.emc.mongoose.model.Constants.PATH_CONFIG_SCHEMA;
import static com.emc.mongoose.model.Constants.PATH_DEFAULTS;
import static com.emc.mongoose.model.Constants.USER_HOME;

public final class InstallHook
implements Runnable {

	private final Path appHomePath;

	public InstallHook() {
		final String appVersion = defaultVersion();
		System.out.println(Constants.APP_NAME + " version " + appVersion);
		appHomePath = Paths.get(USER_HOME, "." + Constants.APP_NAME, appVersion);
		try {
			Files.createDirectories(appHomePath);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
			return;
		}
	}

	private static String defaultVersion()
	throws IllegalStateException {
		final URL defaultConfigUrl = Config.class.getClassLoader().getResource(PATH_DEFAULTS);
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
	}

	private static void installResourcesFile(final Path appHomePath, final String relPath) {
		final Path dstPath = Paths.get(appHomePath.toString(), relPath);
		System.out.print("Checking the file " + dstPath + "... ");
		if(dstPath.toFile().exists()) {
			System.out.println("exists, skipping");
			return;
		}
		dstPath.getParent().toFile().mkdirs();
		try(final InputStream srcFileInput = Config.class.getResourceAsStream(relPath)) {
			final long copiedBytesCount = Files.copy(srcFileInput, dstPath);
			System.out.println("installed (" + SizeInBytes.formatFixedSize(copiedBytesCount) + ")");
		} catch(final IOException e) {
			System.out.println("failed to install (" + e + ")");
		}
	}
}
