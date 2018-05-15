package com.emc.mongoose.config;

import com.github.akurilov.confuse.SchemaProvider;

import java.util.Map;

import static com.emc.mongoose.Constants.APP_NAME;

public final class InitialConfigSchemaProvider
implements SchemaProvider  {

	@Override
	public final String id() {
		return APP_NAME;
	}

	@Override
	public final Map<String, Object> schema()
	throws Exception {
		return ConfigUtil.loadConfigSchema(getClass().getResource("/config-schema.json"));
	}
}
