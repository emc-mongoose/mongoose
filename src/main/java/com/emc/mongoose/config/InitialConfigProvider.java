package com.emc.mongoose.config;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.PATH_DEFAULTS;

import com.github.akurilov.confuse.io.json.JsonConfigProviderBase;

import java.io.File;
import java.io.InputStream;

public class InitialConfigProvider
extends JsonConfigProviderBase {

	@Override
	public String id() {
		return APP_NAME;
	}

	@Override
	protected final InputStream configInputStream() {
		return InitialConfigProvider.class.getResourceAsStream(File.separator + PATH_DEFAULTS);
	}
}
