package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.ui.config.Config;

import java.util.List;
import java.util.Map;

public class LoadStep
extends StepBase {

	public static final String TYPE = "Load";

	public LoadStep(final Config baseConfig) {
		this(baseConfig, null, null);
	}

	protected LoadStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env
	) {
		super(baseConfig, stepConfigs, env);
	}

	@Override
	protected void doStartLocal(final Config actualConfig) {
		// TODO
	}

	@Override
	protected String getTypeName() {
		return TYPE;
	}

	@Override
	protected StepBase copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new LoadStep(baseConfig, stepConfigs, env);
	}
}
