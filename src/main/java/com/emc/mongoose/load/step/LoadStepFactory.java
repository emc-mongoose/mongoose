package com.emc.mongoose.load.step;

import com.github.akurilov.confuse.Config;

import java.util.List;
import java.util.Map;

public interface LoadStepFactory<T extends LoadStep> {

	String getTypeName();

	T create(
		final Config baseConfig, final ClassLoader clsLoader,
		final List<Map<String, Object>> overrides
	);
}
