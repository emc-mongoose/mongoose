package com.emc.mongoose.storage.driver.coop.net.http;

import com.github.akurilov.confuse.io.json.JsonConfigProviderBase;

import java.io.IOException;
import java.io.InputStream;

import static com.emc.mongoose.Constants.APP_NAME;

public final class HttpStorageDriverConfigProvider
extends JsonConfigProviderBase  {

	private static final String CONFIG_

	@Override
	protected final InputStream configInputStream()
	throws IOException {
		return getClass().getResourceAsStream();
	}

	@Override
	public final String id() {
		return APP_NAME;
	}
}
