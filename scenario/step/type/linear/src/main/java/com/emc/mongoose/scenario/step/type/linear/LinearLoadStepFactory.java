package com.emc.mongoose.scenario.step.type.linear;

import com.emc.mongoose.scenario.step.type.LoadStepFactory;
import com.emc.mongoose.ui.config.Config;

import java.util.List;
import java.util.Map;

public class LinearLoadStepFactory<T extends LinearLoadStep>
implements LoadStepFactory<T> {

	@Override
	public String getTypeName() {
		return LinearLoadStep.TYPE;
	}

	@Override @SuppressWarnings("unchecked")
	public T create(final Config baseConfig, final List<Map<String, Object>> overrides) {
		return (T) new LinearLoadStep(baseConfig, overrides);
	}
}
