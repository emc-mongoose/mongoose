package com.emc.mongoose.load.step.type.linear;

import com.emc.mongoose.load.step.LoadStepFactory;

import com.github.akurilov.confuse.Config;

import java.util.List;
import java.util.Map;

public class LinearLoadStepFactory<T extends LinearLoadStep>
implements LoadStepFactory<T> {

	@Override
	public String getTypeName() {
		return LinearLoadStep.TYPE;
	}

	@Override @SuppressWarnings("unchecked")
	public T create(
		final Config baseConfig, final ClassLoader clsLoader,
		final List<Map<String, Object>> overrides
	) {
		return (T) new LinearLoadStep(baseConfig, clsLoader, overrides);
	}
}
