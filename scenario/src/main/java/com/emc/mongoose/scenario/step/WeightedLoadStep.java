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
import com.emc.mongoose.ui.config.load.generator.GeneratorConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.system.SizeInBytes;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 08.11.16.
 */
public class WeightedLoadStep
extends ConfigurableStepBase {

	protected long timeLimitSec = Long.MAX_VALUE;
	protected StepConfig sharedTestStepConfig;

	public WeightedLoadStep(final Config baseConfig) {
		this(baseConfig, null);
	}

	protected WeightedLoadStep(
		final Config baseConfig, final List<Map<String, Object>> stepConfigs
	) {
		super(baseConfig, stepConfigs);
	}

	@Override @SuppressWarnings("unchecked")
	protected Config init() {
		final Config firstConfig = new Config(baseConfig);
		firstConfig.apply(
			stepConfigs.get(0),
			getTypeName() + "_" + LogUtil.getDateTimeStamp() + "_" + hashCode()
		);
		sharedTestStepConfig = firstConfig.getTestConfig().getStepConfig();
		id = sharedTestStepConfig.getId();
		timeLimitSec = sharedTestStepConfig.getLimitConfig().getTime();
		if(timeLimitSec <= 0) {
			timeLimitSec = Long.MAX_VALUE;
		}
		final Config actualConfig = new Config(baseConfig);
		actualConfig.getTestConfig().getStepConfig().setId(id);
		return actualConfig;
	}

	@Override @SuppressWarnings("unchecked")
	protected void invoke(final Config actualConfig)
	throws Throwable {

		Loggers.MSG.info("Run the weighted load step \"{}\"", id);

		final int loadGeneratorCount = stepConfigs.size();
		final Map<LoadGenerator, List<StorageDriver>> driverMap = new HashMap<>(loadGeneratorCount);
		final Int2IntMap weightMap = new Int2IntOpenHashMap(loadGeneratorCount);
		final Map<LoadGenerator, SizeInBytes> itemDataSizes = new HashMap<>(loadGeneratorCount);
		final Map<LoadGenerator, LoadConfig> loadConfigMap = new HashMap<>(loadGeneratorCount);
		final Map<LoadGenerator, OutputConfig> outputConfigMap = new HashMap<>(loadGeneratorCount);

		try {

			for(int i = 0; i < loadGeneratorCount; i ++) {
				
				final Config config = new Config(actualConfig);
				config.apply(stepConfigs.get(i), null);
				final ItemConfig itemConfig = config.getItemConfig();
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final InputConfig dataInputConfig = dataConfig.getInputConfig();
				
				final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
				final LayerConfig dataLayerConfig = dataInputConfig.getLayerConfig();
				final DataInput dataInput = DataInput.getInstance(
					dataInputConfig.getFile(), dataInputConfig.getSeed(),
					dataLayerConfig.getSize(), dataLayerConfig.getCache()
				);
				
				final ItemFactory itemFactory = ItemType.getItemFactory(itemType);
				Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");

				final LoadConfig loadConfig = config.getLoadConfig();
				final GeneratorConfig generatorConfig = loadConfig.getGeneratorConfig();
				final OutputConfig outputConfig = config.getOutputConfig();
				final MetricsConfig metricsConfig = outputConfig.getMetricsConfig();
				final StorageConfig storageConfig = config.getStorageConfig();
				final LimitConfig limitConfig = config
					.getTestConfig().getStepConfig().getLimitConfig();

				final List<StorageDriver> drivers = new ArrayList<>();
				StorageDriverUtil.init(
					drivers, itemConfig, loadConfig, metricsConfig.getAverageConfig(),
					storageConfig, sharedTestStepConfig, dataInput
				);

				final LoadGenerator loadGenerator = new BasicLoadGeneratorBuilder<>()
					.setItemConfig(itemConfig)
					.setItemFactory(itemFactory)
					.setItemType(itemType)
					.setLoadConfig(loadConfig)
					.setLimitConfig(limitConfig)
					.setStorageDrivers(drivers)
					.setAuthConfig(storageConfig.getAuthConfig())
					.build();
				
				driverMap.put(loadGenerator, drivers);
				weightMap.put(loadGenerator.hashCode(), generatorConfig.getWeight());
				itemDataSizes.put(loadGenerator, dataConfig.getSize());
				loadConfigMap.put(loadGenerator, loadConfig);
				outputConfigMap.put(loadGenerator, outputConfig);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to init the content source");
		}

		try(
			final LoadController controller = new BasicLoadController(
				id, driverMap, weightMap, itemDataSizes, loadConfigMap, sharedTestStepConfig,
				outputConfigMap
			)
		) {
			final String itemOutputFile = actualConfig.getItemConfig().getOutputConfig().getFile();
			if(itemOutputFile != null && itemOutputFile.length() > 0) {
				final Path itemOutputPath = Paths.get(itemOutputFile);
				if(Files.exists(itemOutputPath)) {
					Loggers.ERR.warn("Items output file \"{}\" already exists", itemOutputPath);
				}
				// NOTE: using null as an ItemFactory
				final Output itemOutput = new ItemInfoFileOutput<>(itemOutputPath);
				controller.setIoResultsOutput(itemOutput);
			}
			controller.start();
			Loggers.MSG.info("Weighted load step \"{}\" started", controller.getName());
			if(controller.await(timeLimitSec, TimeUnit.SECONDS)) {
				Loggers.MSG.info("Weighted load step \"{}\" done", controller.getName());
			} else {
				Loggers.MSG.info("Weighted load step \"{}\" timeout", controller.getName());
			}
		} catch(final RemoteException e) {
			LogUtil.exception(Level.ERROR, e, "Unexpected failure");
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to open the item output file");
		}
	}

	@Override
	protected StepBase copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new WeightedLoadStep(baseConfig, stepConfigs);
	}

	@Override
	protected String getTypeName() {
		return "weighted";
	}
}
