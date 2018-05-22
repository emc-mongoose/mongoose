package com.emc.mongoose.env;

import com.emc.mongoose.config.BundledDefaultsProvider;
import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.USER_HOME;
import static com.emc.mongoose.config.CliArgUtil.ARG_PATH_SEP;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class MainInstaller
extends JarResourcesInstaller {

	private final Path appHomePath;

	public MainInstaller() {
		final Config bundledDefaults;
		try {
			final Map<String, Object> schema = SchemaProvider.resolveAndReduce(
				APP_NAME, Thread.currentThread().getContextClassLoader()
			);
			bundledDefaults = new BundledDefaultsProvider().config(ARG_PATH_SEP, schema);
		} catch(final Exception e) {
			throw new IllegalStateException(
				"Failed to load the bundled default config from the resources", e
			);
		}
		final String appVersion = bundledDefaults.stringVal("run-version");
		System.out.println(APP_NAME + " v " + appVersion);
		appHomePath = Paths.get(USER_HOME, "." + APP_NAME, appVersion);
	}

	public final Path appHomePath() {
		return appHomePath;
	}
}
