package com.emc.mongoose.scenario.step.type.weighted;

import com.emc.mongoose.scenario.step.type.LoadStepFactory;
import com.emc.mongoose.config.Config;

import java.util.List;
import java.util.Map;

public class WeightedLoadStepFactory<T extends WeightedLoadStep>
implements LoadStepFactory<T> {

	@Override
	public String getTypeName() {
		return WeightedLoadStep.TYPE;
	}

	@Override @SuppressWarnings("unchecked")
	public T create(final Config baseConfig, final List<Map<String, Object>> overrides) {
		return (T) new WeightedLoadStep(baseConfig, overrides);
	}
}
