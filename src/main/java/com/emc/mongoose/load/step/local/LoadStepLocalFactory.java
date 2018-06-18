package com.emc.mongoose.load.step.local;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.load.step.LoadStep;

import com.github.akurilov.confuse.Config;

import java.util.List;
import java.util.Map;

public interface LoadStepLocalFactory<T extends LoadStep> {

	T create(
		final Config baseConfig, final List<Extension> extensions,
		final List<Map<String, Object>> overrides
	);
}
