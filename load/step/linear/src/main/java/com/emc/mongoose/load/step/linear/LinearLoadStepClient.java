package com.emc.mongoose.load.step.linear;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.load.step.LoadStepBase;
import com.emc.mongoose.load.step.client.LoadStepClientBase;

import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;

import java.util.List;
import java.util.Map;

import static com.emc.mongoose.load.step.linear.LinearLoadStep.initConfig;

public class LinearLoadStepClient
extends LoadStepClientBase {

	public LinearLoadStepClient(
		final Config baseConfig, final List<Extension> extensions, final List<Map<String, Object>> stepConfigs
	) {
		super(baseConfig, extensions, stepConfigs);
	}

	@Override
	protected LoadStepBase copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new LinearLoadStepClient(baseConfig, extensions, stepConfigs);
	}

	@Override
	protected void init()
	throws IllegalStateException {

		final Config config = initConfig(baseConfig, stepConfigs);
		actualConfig(config);

		final Config loadConfig = config.configVal("load");
		final Config stepConfig = loadConfig.configVal("step");
		final IoType ioType = IoType.valueOf(loadConfig.stringVal("type").toUpperCase());
		final int concurrency = stepConfig.intVal("limit-concurrency");
		final Config outputConfig = config.configVal("output");
		final Config metricsConfig = outputConfig.configVal("metrics");
		final SizeInBytes itemDataSize = new SizeInBytes(config.stringVal("item-data-size"));
		final int originIndex = 0;
		final boolean colorFlag = outputConfig.boolVal("color");

		initMetrics(originIndex, ioType, concurrency, metricsConfig, itemDataSize, colorFlag);
	}

	@Override
	public String getTypeName() {
		return LinearLoadStepExtension.TYPE;
	}
}
