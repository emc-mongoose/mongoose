package com.emc.mongoose.base.config;

import static com.emc.mongoose.base.Constants.APP_NAME;

import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.io.yaml.YamlSchemaProviderBase;

import java.io.IOException;
import java.io.InputStream;

public final class InitialConfigSchemaProvider extends YamlSchemaProviderBase {

	@Override
	public final String id() {
		return APP_NAME;
	}

	@Override
	protected final InputStream schemaInputStream() throws IOException {
		return getClass().getResource("/config-schema.yaml").openStream();
	}

	public static SchemaProvider provider() {
		return new InitialConfigSchemaProvider();
	}
}
