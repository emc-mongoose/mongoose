package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemInfoFileOutput;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.concurrent.RateThrottle;
import com.github.akurilov.concurrent.WeightThrottle;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

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

		final var autoStepId = getTypeName().toLowerCase() + "_" + LogUtil.getDateTimeStamp();
		final var config = new Config(baseConfig);
		final var stepConfig = config.getTestConfig().getStepConfig();
		if(stepConfig.getIdTmp()) {
			stepConfig.setId(autoStepId);
		}
		actualConfig(config);
	}

	@Override
	protected void doStartLocal(final Config actualConfig) {

	}
}
