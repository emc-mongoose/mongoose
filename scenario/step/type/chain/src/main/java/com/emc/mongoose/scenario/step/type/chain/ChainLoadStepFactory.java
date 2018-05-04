package com.emc.mongoose.scenario.step.type.chain;

import com.emc.mongoose.scenario.step.LoadStepFactory;
import com.emc.mongoose.config.Config;

import java.util.List;
import java.util.Map;

public class ChainLoadStepFactory<T extends ChainLoadStep>
implements LoadStepFactory<T> {

	@Override
	public String getTypeName() {
		return ChainLoadStep.TYPE;
	}

	@Override @SuppressWarnings("unchecked")
	public T create(final Config baseConfig, final List<Map<String, Object>> overrides) {
		return (T) new ChainLoadStep(baseConfig, overrides);
	}
}
