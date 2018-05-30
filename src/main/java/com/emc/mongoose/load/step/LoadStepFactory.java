package com.emc.mongoose.load.step;

import com.emc.mongoose.env.Extension;

import com.github.akurilov.confuse.Config;

import java.util.List;
import java.util.Map;

public interface LoadStepFactory<T extends LoadStep>
extends Extension {

	T create(
		final Config baseConfig, final List<Extension> extensions,
		final List<Map<String, Object>> overrides
	);
}
