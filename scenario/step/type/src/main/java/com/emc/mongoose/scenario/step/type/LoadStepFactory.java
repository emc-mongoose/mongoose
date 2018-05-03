package com.emc.mongoose.scenario.step.type;

import com.emc.mongoose.scenario.step.LoadStep;
import com.emc.mongoose.config.Config;

import java.util.List;
import java.util.Map;

public interface LoadStepFactory<T extends LoadStep> {

	String getTypeName();

	T create(final Config baseConfig, final List<Map<String, Object>> overrides);
}
