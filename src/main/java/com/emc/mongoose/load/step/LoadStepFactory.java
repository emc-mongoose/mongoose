package com.emc.mongoose.load.step;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.load.step.client.LoadStepClient;

import com.github.akurilov.confuse.Config;

import java.util.List;
import java.util.Map;

public interface LoadStepFactory<T extends LoadStep, U extends LoadStepClient>
extends Extension {

	T createLocal(final Config baseConfig, final List<Extension> extensions, final List<Map<String, Object>> overrides);

	U createClient(
		final Config baseConfig, final List<Extension> extensions, final List<Map<String, Object>> overrides
	);
}
