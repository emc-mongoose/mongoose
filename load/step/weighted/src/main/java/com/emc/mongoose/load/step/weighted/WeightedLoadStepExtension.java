package com.emc.mongoose.load.step.weighted;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.env.ExtensionBase;
import com.emc.mongoose.load.step.LoadStepFactory;
import static com.emc.mongoose.Constants.APP_NAME;

import com.emc.mongoose.metrics.MetricsManager;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class WeightedLoadStepExtension<T extends WeightedLoadStepLocal, U extends WeightedLoadStepClient>
extends ExtensionBase
implements LoadStepFactory<T, U> {

	public static final String TYPE = "WeightedLoad";

	private static final SchemaProvider SCHEMA_PROVIDER = new JsonSchemaProviderBase() {

		@Override
		protected final InputStream schemaInputStream() {
			return getClass().getResourceAsStream("/config-schema-load-generator-weight.json");
		}

		@Override
		public final String id() {
			return APP_NAME;
		}
	};

	private static final String DEFAULTS_FILE_NAME = "defaults-load-generator-weight.json";

	private static final List<String> RES_INSTALL_FILES = Collections.unmodifiableList(
		Arrays.asList("config/" + DEFAULTS_FILE_NAME)
	);

	@Override
	public final String id() {
		return TYPE;
	}

	@Override @SuppressWarnings("unchecked")
	public final T createLocal(
		final Config baseConfig, final List<Extension> extensions, final List<Config> contextConfigs,
		final MetricsManager metricsManager
	) {
		return (T) new WeightedLoadStepLocal(baseConfig, extensions, contextConfigs, metricsManager);
	}

	@Override @SuppressWarnings("unchecked")
	public final U createClient(
		final Config baseConfig, final List<Extension> extensions, final MetricsManager metricsManager
	) {
		return (U) new WeightedLoadStepClient(baseConfig, extensions, null, metricsManager);
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
}
