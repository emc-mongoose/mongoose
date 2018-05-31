package com.emc.mongoose.env;

import com.emc.mongoose.config.ConfigUtil;
import static com.emc.mongoose.Constants.DIR_CONFIG;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import org.apache.logging.log4j.Level;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public abstract class ExtensionBase
extends JarResourcesInstaller
implements Extension {

	@Override
	public final void install(final Path appHomePath) {
		Loggers.MSG.info(
			"Check/install the extension: \"{}\" ({})", id(), getClass().getCanonicalName()
		);
		accept(appHomePath);
	}

	@Override
	public final Config defaults(final Path appHomePath) {

		final SchemaProvider schemaProvider = schemaProvider();
		final Map<String, Object> schema;
		if(schemaProvider == null) {
			schema = null;
		} else {
			try {
				schema = schemaProvider.schema();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "{}: failed to load the schema", schemaProvider);
				return null;
			}
		}

		final String defaultsFileName = defaultsFileName();
		if(defaultsFileName == null) {
			return null;
		}
		final File defaultsFile = Paths
			.get(appHomePath.toString(), DIR_CONFIG, defaultsFileName)
			.toFile();
		try {
			return ConfigUtil.loadConfig(defaultsFile, schema);
		} catch(final Exception e) {
			LogUtil.exception(
				Level.WARN, e, "Failed to load the defaults config from \"{}\"", defaultsFile
			);
			return null;
		}
	}

	protected abstract String defaultsFileName();
}
