package com.emc.mongoose.config;

import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;

import java.io.IOException;
import java.io.InputStream;

import static com.emc.mongoose.Constants.APP_NAME;

public final class InitialConfigSchemaProvider
extends JsonSchemaProviderBase  {

	@Override
	public final String id() {
		return APP_NAME;
	}

	@Override
	protected final InputStream schemaInputStream()
	throws IOException {
		return getClass().getResource("/config-schema.json").openStream();
	}
}
