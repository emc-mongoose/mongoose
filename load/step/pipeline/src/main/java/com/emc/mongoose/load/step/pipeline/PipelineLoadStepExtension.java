package com.emc.mongoose.load.step.pipeline;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.env.ExtensionBase;
import com.emc.mongoose.load.step.LoadStepFactory;
import com.emc.mongoose.load.step.client.LoadStepClient;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.emc.mongoose.Constants.APP_NAME;

public final class PipelineLoadStepExtension<T extends PipelineLoadStepLocal, U extends PipelineLoadStepClient>
extends ExtensionBase
implements LoadStepFactory<T, U> {

	public static final String TYPE = "PipelineLoad";

	private static final SchemaProvider SCHEMA_PROVIDER = new JsonSchemaProviderBase() {

		@Override
		protected final InputStream schemaInputStream() {
			return getClass().getResourceAsStream("/config-schema-item-output-delay.json");
		}

		@Override
		public final String id() {
			return APP_NAME;
		}
	};

	private static final String DEFAULTS_FILE_NAME = "defaults-item-output-delay.json";

	private static final List<String> RES_INSTALL_FILES = Collections.unmodifiableList(
		Arrays.asList("config/" + DEFAULTS_FILE_NAME)
	);

	@Override
	public final String id() {
		return TYPE;
	}

	@Override
	public final SchemaProvider schemaProvider() {
		return SCHEMA_PROVIDER;
	}

	@Override
	protected final String defaultsFileName() {
		return DEFAULTS_FILE_NAME;
	}

	@Override
	protected final List<String> resourceFilesToInstall() {
		return RES_INSTALL_FILES;
	}

	@Override @SuppressWarnings("unchecked")
	public final T createLocal(
		final Config baseConfig, final List<Extension> extensions, final List<Map<String, Object>> overrides
	) {
		return (T) new PipelineLoadStepLocal(baseConfig, extensions, overrides);
	}

	@Override @SuppressWarnings("unchecked")
	public final U createClient(
		final Config baseConfig, final List<Extension> extensions, final List<Map<String, Object>> overrides
	) {
		return (U) new PipelineLoadStepClient(baseConfig, extensions, overrides);
	}
}
