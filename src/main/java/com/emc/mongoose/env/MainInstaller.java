package com.emc.mongoose.env;

import com.emc.mongoose.Main;
import com.emc.mongoose.config.ConfigUtil;
import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.PATH_DEFAULTS;
import static com.emc.mongoose.Constants.USER_HOME;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class MainInstaller
extends JarResourcesInstaller {

	private final Path appHomePath;

	public MainInstaller() {
		final URL defaultConfigUrl = Main.class.getResource(
			File.separator + RESOURCES_TO_INSTALL_PREFIX + File.separator + PATH_DEFAULTS
		);
		if(defaultConfigUrl == null) {
			throw new IllegalStateException("No bundled default config found");
		}
		final Config bundledDefaults;
		try {
			final Map<String, Object> schema = SchemaProvider.resolveAndReduce(
				APP_NAME, Main.class.getClassLoader()
			);
			bundledDefaults = ConfigUtil.loadConfig(defaultConfigUrl, schema);
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

	@Override
	protected InputStream resourceStream(final String resPath) {
		return MainInstaller.class.getResourceAsStream(resPath);
	}
}
