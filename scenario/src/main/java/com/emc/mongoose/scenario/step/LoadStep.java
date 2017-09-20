package com.emc.mongoose.scenario.step;

import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemInfoFileOutput;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverUtil;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.data.input.InputConfig;
import com.emc.mongoose.ui.config.item.data.input.layer.LayerConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.system.SizeInBytes;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 14.09.17.
 */
public class LoadStep
extends ConfigurableStepBase
implements Step {

	public LoadStep(final Config baseConfig) {
		this(baseConfig, null);
	}

	protected LoadStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs
	) {
		super(baseConfig, stepConfigs);
	}

	@Override
	protected LoadStep copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new LoadStep(baseConfig, stepConfigs);
	}

	@Override
	protected String getTypeName() {
		return "load";
	}

	@Override
	protected void invoke(final Config config)
	throws Throwable {

		final StepConfig stepConfig = config.getTestConfig().getStepConfig();
		final String stepId = stepConfig.getId();
		Loggers.MSG.info("Run the load step \"{}\"", stepId);
		final LoadConfig loadConfig = config.getLoadConfig();
		final LimitConfig limitConfig = stepConfig.getLimitConfig();
		final OutputConfig outputConfig = config.getOutputConfig();
		final MetricsConfig metricsConfig = outputConfig.getMetricsConfig();
		final AverageConfig avgMetricsConfig = metricsConfig.getAverageConfig();
		final ItemConfig itemConfig = config.getItemConfig();
		final DataConfig dataConfig = itemConfig.getDataConfig();
		final InputConfig dataInputConfig = dataConfig.getInputConfig();
		final StorageConfig storageConfig = config.getStorageConfig();
		final LayerConfig dataLayerConfig = dataInputConfig.getLayerConfig();

		final DataInput dataInput = DataInput.getInstance(
			dataInputConfig.getFile(), dataInputConfig.getSeed(), dataLayerConfig.getSize(),
			dataLayerConfig.getCache()
		);

		final List<StorageDriver> drivers = new ArrayList<>();
		StorageDriverUtil.init(
			drivers, itemConfig, loadConfig, avgMetricsConfig, storageConfig, stepConfig,
			dataInput
		);

		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final ItemFactory itemFactory = ItemType.getItemFactory(itemType);
		Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");

		final LoadGenerator loadGenerator = new BasicLoadGeneratorBuilder<>()
			.setItemConfig(itemConfig)
			.setLoadConfig(loadConfig)
			.setLimitConfig(limitConfig)
			.setItemType(itemType)
			.setItemFactory(itemFactory)
			.setStorageDrivers(drivers)
			.setAuthConfig(storageConfig.getAuthConfig())
			.build();
		Loggers.MSG.info("Load generators initialized");

		final long timeLimitSec;
		long t = limitConfig.getTime();
		if(t > 0) {
			timeLimitSec = t;
		} else {
			timeLimitSec = Long.MAX_VALUE;
		}

		final Map<LoadGenerator, List<StorageDriver>> driversMap = new HashMap<>();
		driversMap.put(loadGenerator, drivers);
		final Map<LoadGenerator, SizeInBytes> itemDataSizes = new HashMap<>();
		itemDataSizes.put(loadGenerator, dataConfig.getSize());
		final Map<LoadGenerator, LoadConfig> loadConfigMap = new HashMap<>();
		loadConfigMap.put(loadGenerator, loadConfig);
		final Map<LoadGenerator, OutputConfig> outputConfigMap = new HashMap<>();
		outputConfigMap.put(loadGenerator, outputConfig);
		try(
			final LoadController controller = new BasicLoadController(
				stepId, driversMap, null, itemDataSizes, loadConfigMap, stepConfig, outputConfigMap
			)
		) {
			final String itemOutputFile = itemConfig.getOutputConfig().getFile();
			if(itemOutputFile != null && itemOutputFile.length() > 0) {
				final Path itemOutputPath = Paths.get(itemOutputFile);
				if(Files.exists(itemOutputPath)) {
					Loggers.ERR.warn("Items output file \"{}\" already exists", itemOutputPath);
				}
				final Output itemOutput = new ItemInfoFileOutput<>(itemOutputPath);
				controller.setIoResultsOutput(itemOutput);
			}
			controller.start();
			Loggers.MSG.info("Load step \"{}\" started", stepId);
			if(controller.await(timeLimitSec, TimeUnit.SECONDS)) {
				Loggers.MSG.info("Load step \"{}\" done", stepId);
			} else {
				Loggers.MSG.info("Load step \"{}\" timeout", stepId);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to open the item output file");
		}
	}
}
