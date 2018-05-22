package com.emc.mongoose.storage.driver.coop.net.http;

import com.emc.mongoose.env.ExtensionBase;

import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;

import java.io.InputStream;

import static com.emc.mongoose.Constants.APP_NAME;

public final class HttpStorageDriverExtension
extends ExtensionBase {

	private static final String DEFAULTS_FILE_NAME = "defaults-storage-net-http.json";
	private static final SchemaProvider SCHEMA_PROVIDER = new JsonSchemaProviderBase() {

		@Override
		protected final InputStream schemaInputStream() {
			return getClass().getResourceAsStream("/config-schema-storage-net-http.json");
		}

		@Override
		public final String id() {
			return APP_NAME;
		}
	};

	@Override
	protected final SchemaProvider schemaProvider() {
		return SCHEMA_PROVIDER;
	}

	@Override
	protected final String defaultsFileName() {
		return DEFAULTS_FILE_NAME;
	}

	@Override
	public String id() {
		return "http";
	}
}
