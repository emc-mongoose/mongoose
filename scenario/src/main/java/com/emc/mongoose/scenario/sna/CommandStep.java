package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CommandStep
extends StepBase {

	private String cmd = null;

	public CommandStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env
	) {
		super(baseConfig, stepConfigs, env);
	}

	private CommandStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs,
		final Map<String, String> env, final String cmd
	) {
		super(baseConfig, stepConfigs, env);
		this.cmd = cmd;
	}

	@Override
	protected final void doStart(final Config actualConfig)
	throws InterruptedException {

	}

	@Override
	protected final void doStop() {
	}

	@Override
	protected final void doClose() {
	}

	@Override
	protected final String getTypeName() {
		return "command";
	}

	@Override
	protected final CommandStep copyInstance(final Object config) {
		if(!(config instanceof String)) {
			throw new IllegalArgumentException(
				getTypeName() + " step type accepts only string parameter for the config method"
			);
		}
		return new CommandStep(baseConfig, stepConfigs, env, (String) config);
	}
}
