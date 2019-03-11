package com.emc.mongoose.storage.driver.coop.netty.http;

import com.emc.mongoose.base.env.ExtensionBase;

import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.io.yaml.YamlSchemaProviderBase;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.emc.mongoose.base.Constants.APP_NAME;

public final class HttpStorageDriverExtension
				extends ExtensionBase {

	private static final SchemaProvider SCHEMA_PROVIDER = new YamlSchemaProviderBase() {

		@Override
		protected final InputStream schemaInputStream() {
			return getClass().getResourceAsStream("/config-schema-storage-net-http.yaml");
		}

		@Override
		public final String id() {
			return APP_NAME;
		}
	};

	private static final String DEFAULTS_FILE_NAME = "defaults-storage-net-http.yaml";

	private static final List<String> RES_INSTALL_FILES = Collections.unmodifiableList(
					Arrays.asList(
									"config/" + DEFAULTS_FILE_NAME));

	@Override
	public final SchemaProvider schemaProvider() {
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

	@Override
	protected final List<String> resourceFilesToInstall() {
		return RES_INSTALL_FILES;
	}
}
