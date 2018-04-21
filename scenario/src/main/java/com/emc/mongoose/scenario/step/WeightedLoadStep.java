package com.emc.mongoose.scenario.step;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.log.LogUtil;

import java.util.List;
import java.util.Map;

public class WeightedLoadStep
extends LoadStepBase {

	public static final String TYPE = "WeightedLoad";

	public WeightedLoadStep(final Config baseConfig) {
		super(baseConfig, null);
	}

	public WeightedLoadStep(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		super(baseConfig, stepConfigs);
	}

	@Override
	public String getTypeName() {
		return TYPE;
	}

	@Override
	protected WeightedLoadStep copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new WeightedLoadStep(baseConfig, stepConfigs);
	}

	@Override
	protected void init() {

		final String autoStepId = getTypeName().toLowerCase() + "_" + LogUtil.getDateTimeStamp();
		final Config config = new Config(baseConfig);
		final StepConfig stepConfig = config.getTestConfig().getStepConfig();
		if(stepConfig.getIdTmp()) {
			stepConfig.setId(autoStepId);
		}
		actualConfig(config);
	}

	@Override
	protected void doStartLocal(final Config actualConfig) {

	}
}
