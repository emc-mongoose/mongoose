package com.emc.mongoose.config;

import com.github.akurilov.confuse.io.json.JsonConfigProviderBase;

import java.io.File;
import java.io.InputStream;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.PATH_DEFAULTS;
import static com.emc.mongoose.env.Installer.RESOURCES_TO_INSTALL_PREFIX;

public class BundledDefaultsProvider
extends JsonConfigProviderBase {

	@Override
	protected final InputStream configInputStream() {
		return getClass().getResourceAsStream(
			File.separator + RESOURCES_TO_INSTALL_PREFIX + File.separator + PATH_DEFAULTS
		);
	}

	@Override
	public final String id() {
		return APP_NAME;
	}
}
