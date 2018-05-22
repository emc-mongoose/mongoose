package com.emc.mongoose.storage.driver.coop.net;

import com.emc.mongoose.env.ExtensionBase;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;

import java.io.InputStream;

import static com.emc.mongoose.Constants.APP_NAME;

public final class NetStorageDriverExtension
extends ExtensionBase {

	private static final String DEFAULTS_FILE_NAME = "defaults-storage-net.json";
	private static final SchemaProvider SCHEMA_PROVIDER = new JsonSchemaProviderBase() {

		@Override
		protected final InputStream schemaInputStream() {
			return getClass().getResourceAsStream("/config-schema-storage-net.json");
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
		return "net";
	}
}
