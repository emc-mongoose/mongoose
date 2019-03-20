package com.emc.mongoose.base.env;

import static com.emc.mongoose.base.Constants.DIR_CONFIG;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;

import com.emc.mongoose.base.config.ConfigUtil;
import com.emc.mongoose.base.logging.LogUtil;
import com.github.akurilov.confuse.Config;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.logging.log4j.Level;

public abstract class ExtensionBase extends InstallableJarResources implements Extension {

	@Override
	public final Config defaults(final Path appHomePath) {

		final var schemaProvider = schemaProvider();
		final Map<String, Object> schema;
		if (schemaProvider == null) {
			schema = null;
		} else {
			try {
				schema = schemaProvider.schema();
			} catch (final Exception e) {
				LogUtil.exception(Level.WARN, e, "{}: failed to load the schema", schemaProvider);
				return null;
			}
		}

		final var defaultsFileName = defaultsFileName();
		if (defaultsFileName == null) {
			return null;
		}
		final var defaultsFile = Paths.get(appHomePath.toString(), DIR_CONFIG, defaultsFileName).toFile();
		try {
			return ConfigUtil.loadConfig(defaultsFile, schema);
		} catch (final Exception e) {
			throwUncheckedIfInterrupted(e);
			LogUtil.exception(
							Level.WARN, e, "Failed to load the defaults config from \"{}\"", defaultsFile);
			return null;
		}
	}

	protected abstract String defaultsFileName();
}
