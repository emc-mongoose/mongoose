package com.emc.mongoose.load.step.type.weighted;

import com.emc.mongoose.load.step.LoadStepFactory;

import com.github.akurilov.confuse.Config;

import java.util.List;
import java.util.Map;

public class WeightedLoadStepFactory<T extends WeightedLoadStep>
implements LoadStepFactory<T> {

	@Override
	public String getTypeName() {
		return WeightedLoadStep.TYPE;
	}

	@Override @SuppressWarnings("unchecked")
	public T create(
		final Config baseConfig, final ClassLoader clsLoader,
		final List<Map<String, Object>> overrides
	) {
		return (T) new WeightedLoadStep(baseConfig, clsLoader, overrides);
	}
}
