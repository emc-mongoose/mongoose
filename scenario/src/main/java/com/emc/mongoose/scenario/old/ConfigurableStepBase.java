package com.emc.mongoose.scenario.old;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ConfigurableStepBase
extends StepBase
implements ConfigurableStep {

	protected final List<Map<String, Object>> stepConfigs;

	protected ConfigurableStepBase(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs
	) {
		super(baseConfig);
		this.stepConfigs = stepConfigs;
	}

	@Override
	public StepBase config(final Map<String, Object> stepConfig) {
		final List<Map<String, Object>> stepConfigsCopy = new ArrayList<>();
		if(stepConfigs != null) {
			stepConfigsCopy.addAll(stepConfigs);
		}
		if(stepConfig != null) {
			stepConfigsCopy.add(stepConfig);
		}
		return copyInstance(stepConfigsCopy);
	}

	@Override
	public void close()
	throws IOException {
		if(stepConfigs != null) {
			stepConfigs.clear();
		}
	}

	protected abstract StepBase copyInstance(final List<Map<String, Object>> stepConfigs);

	protected Config init() {
		final String autoStepId = getTypeName() + "_" + LogUtil.getDateTimeStamp() + "_"
			+ hashCode();
		final Config config = new Config(baseConfig);
		if(stepConfigs != null && stepConfigs.size() > 0) {
			for(final Map<String, Object> nextStepConfig : stepConfigs) {
				config.apply(nextStepConfig, autoStepId);
			}
		}
		id = config.getTestConfig().getStepConfig().getId();
		return config;
	}
}
