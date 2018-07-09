package com.emc.mongoose.load.step.linear;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.load.step.client.LoadStepClient;
import com.emc.mongoose.load.step.client.LoadStepClientBase;
import com.emc.mongoose.logging.LogUtil;

import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;

import java.util.List;

public class LinearLoadStepClient
extends LoadStepClientBase {

	public LinearLoadStepClient(
		final Config baseConfig, final List<Extension> extensions, final List<Config> contextConfigs
	) {
		super(baseConfig, extensions, contextConfigs);
	}

	@Override @SuppressWarnings("unchecked")
	protected <T extends LoadStepClient> T copyInstance(final Config config, final List<Config> ctxConfigs) {
		return (T) new LinearLoadStepClient(config, extensions, ctxConfigs);
	}

	@Override
	protected void init()
	throws IllegalStateException {

		final String autoStepId = "linear_" + LogUtil.getDateTimeStamp();
		if(config.boolVal("load-step-idAutoGenerated")) {
			config.val("load-step-id", autoStepId);
		}

		final Config loadConfig = config.configVal("load");
		final Config stepConfig = loadConfig.configVal("step");
		final IoType ioType = IoType.valueOf(loadConfig.stringVal("type").toUpperCase());
		final int concurrency = stepConfig.intVal("limit-concurrency");
		final Config outputConfig = config.configVal("output");
		final Config metricsConfig = outputConfig.configVal("metrics");
		final SizeInBytes itemDataSize;
		final Object itemDataSizeRaw = config.val("item-data-size");
		if(itemDataSizeRaw instanceof String) {
			itemDataSize = new SizeInBytes((String) itemDataSizeRaw);
		} else {
			itemDataSize = new SizeInBytes(TypeUtil.typeConvert(itemDataSizeRaw, long.class));
		}
		final int originIndex = 0;
		final boolean colorFlag = outputConfig.boolVal("color");

		initMetrics(originIndex, ioType, concurrency, metricsConfig, itemDataSize, colorFlag);
	}

	@Override
	public String getTypeName() {
		return LinearLoadStepExtension.TYPE;
	}
}
