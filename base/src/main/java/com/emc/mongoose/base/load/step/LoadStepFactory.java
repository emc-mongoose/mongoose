package com.emc.mongoose.base.load.step;

import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.load.step.client.LoadStepClient;
import com.emc.mongoose.base.metrics.MetricsManager;
import com.github.akurilov.confuse.Config;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface LoadStepFactory<T extends LoadStep, U extends LoadStepClient> extends Extension {

	T createLocal(
					final Config baseConfig,
					final List<Extension> extensions,
					final List<Config> contextConfigs,
					final MetricsManager metricsManager);

	U createClient(
					final Config baseConfig,
					final List<Extension> extensions,
					final MetricsManager metricsManager);

	@SuppressWarnings("unchecked")
	static <T extends LoadStep> T createLocalLoadStep(
					final Config baseConfig,
					final List<Extension> extensions,
					final List<Config> contextConfigs,
					final MetricsManager metricsManager,
					final String stepType) {

		final List<LoadStepFactory> loadStepFactories = extensions.stream()
						.filter(ext -> ext instanceof LoadStepFactory)
						.map(ext -> (LoadStepFactory) ext)
						.collect(Collectors.toList());

		final LoadStepFactory selectedFactory = loadStepFactories.stream()
						.filter(f -> stepType.equals(f.id()))
						.findFirst()
						.orElseThrow(
										() -> new IllegalStateException(
														"Failed to find the load step implementation for type \""
																		+ stepType
																		+ "\", available types: "
																		+ Arrays.toString(
																						loadStepFactories.stream().map(LoadStepFactory::id).toArray())));

		return (T) selectedFactory.createLocal(baseConfig, extensions, contextConfigs, metricsManager);
	}
}
