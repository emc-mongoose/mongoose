package com.emc.mongoose.storage.driver.coop.net.http;

import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;

import java.io.IOException;
import java.io.InputStream;

import static com.emc.mongoose.Constants.APP_NAME;

public final class HttpStorageDriverConfigSchemaProvider
extends JsonSchemaProviderBase {

	@Override
	protected final InputStream schemaInputStream()
	throws IOException {
		return getClass().getResource("/config-schema-storage-net-http.json").openStream();
	}

	@Override
	public final String id() {
		return APP_NAME;
	}
}
