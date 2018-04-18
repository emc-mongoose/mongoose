package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.metrics.AggregatingMetricsContext;
import com.emc.mongoose.api.metrics.BasicMetricsContext;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemInfoFileOutput;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class LinearLoadStep
extends LoadStepBase {

	public static final String TYPE = "Load";

	public LinearLoadStep(final Config baseConfig) {
		this(baseConfig, null);
	}

	protected LinearLoadStep(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		super(baseConfig, stepConfigs);
	}

	@Override
	protected void init() {

		final var autoStepId = getTypeName().toLowerCase() + "_" + LogUtil.getDateTimeStamp();
		final var config = new Config(baseConfig);
		if(stepConfigs == null || stepConfigs.size() == 0) {
			final var stepConfig = config.getTestConfig().getStepConfig();
			if(stepConfig.getIdTmp()) {
				stepConfig.setId(autoStepId);
			}
		} else {
			stepConfigs.forEach(nextStepConfig -> config.apply(nextStepConfig, autoStepId));
		}
		actualConfig(config);

		final var stepConfig = config.getTestConfig().getStepConfig();
		final var nodeCount = stepConfig.getNodeConfig().getAddrs().size();
		final var ioType = IoType.valueOf(config.getLoadConfig().getType().toUpperCase());
		final var concurrency = config.getLoadConfig().getLimitConfig().getConcurrency();
		final var outputConfig = config.getOutputConfig();
		final var metricsConfig = outputConfig.getMetricsConfig();
		final var itemDataSize = config.getItemConfig().getDataConfig().getSize();

		if(distributedFlag) {
			metricsContexts.add(
				new AggregatingMetricsContext(
					id(),
					ioType,
					nodeCount,
					concurrency * nodeCount,
					(int) (concurrency * nodeCount * metricsConfig.getThreshold()),
					itemDataSize,
					(int) metricsConfig.getAverageConfig().getPeriod(),
					outputConfig.getColor(),
					metricsConfig.getAverageConfig().getPersist(),
					metricsConfig.getSummaryConfig().getPersist(),
					metricsConfig.getSummaryConfig().getPerfDbResultsFile(),
					() -> stepClient.remoteMetricsSnapshots(0)
				)
			);
		} else {
			metricsContexts.add(
				new BasicMetricsContext(
					id(),
					ioType,
					() -> drivers.stream().mapToInt(StorageDriver::getActiveTaskCount).sum(),
					concurrency,
					(int) (concurrency * metricsConfig.getThreshold()),
					itemDataSize,
					(int) metricsConfig.getAverageConfig().getPeriod(),
					outputConfig.getColor(),
					metricsConfig.getAverageConfig().getPersist(),
					metricsConfig.getSummaryConfig().getPersist(),
					metricsConfig.getSummaryConfig().getPerfDbResultsFile()
				)
			);
		}
	}

	@Override
	protected void doStartLocal(final Config actualConfig) {

		final var itemConfig = actualConfig.getItemConfig();
		final var loadConfig = actualConfig.getLoadConfig();
		final var storageConfig = actualConfig.getStorageConfig();
		final var testConfig = actualConfig.getTestConfig();

		final var dataConfig = itemConfig.getDataConfig();
		final var stepConfig = testConfig.getStepConfig();

		final var dataInputConfig = dataConfig.getInputConfig();
		final var limitConfig = stepConfig.getLimitConfig();

		final var dataLayerConfig = dataInputConfig.getLayerConfig();
		final var outputConfig = actualConfig.getOutputConfig();
		final var testStepId = stepConfig.getId();
		Loggers.MSG.info("Run the load step \"{}\"", testStepId);

		try {
			dataInputs.add(
				DataInput.getInstance(
					dataInputConfig.getFile(), dataInputConfig.getSeed(), dataLayerConfig.getSize(),
					dataLayerConfig.getCache()
				)
			);
		} catch(final IOException e) {
			LogUtil.exception(Level.FATAL, e, "Failed to initialize the data input");
			throw new IllegalStateException("Failed to initialize the data input");
		}

		final var itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final var itemFactory = ItemType.getItemFactory(itemType);
		Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");

		try {
			drivers.add(
				new BasicStorageDriverBuilder<>()
					.testStepId(testStepId)
					.itemConfig(itemConfig)
					.dataInput(dataInputs.get(0))
					.loadConfig(loadConfig)
					.storageConfig(storageConfig)
					.build()
			);
		} catch(final OmgShootMyFootException e) {
			LogUtil.exception(Level.FATAL, e, "Failed to initialize the storage driver");
			throw new IllegalStateException("Failed to initialize the storage driver");
		} catch(final InterruptedException e) {
			throw new CancellationException();
		}

		try {
			generators.add(
				new BasicLoadGeneratorBuilder<>()
					.itemConfig(itemConfig)
					.loadConfig(loadConfig)
					.limitConfig(limitConfig)
					.itemType(itemType)
					.itemFactory((ItemFactory) itemFactory)
					.storageDriver(drivers.get(0))
					.authConfig(storageConfig.getAuthConfig())
					.originIndex(0)
					.build()
			);
		} catch(final OmgShootMyFootException e) {
			LogUtil.exception(Level.FATAL, e, "Failed to initialize the load generator");
			throw new IllegalStateException("Failed to initialize the load generator");
		}

		final var loadConfigs = new ArrayList<LoadConfig>();
		loadConfigs.add(loadConfig);
		final var outputConfigs = new ArrayList<OutputConfig>();
		outputConfigs.add(outputConfig);

		final var controller = new BasicLoadController(
			testStepId, generators, drivers, null, metricsContexts, loadConfigs, stepConfig,
			outputConfigs
		);
		controllers.add(controller);

		final var itemOutputFile = itemConfig.getOutputConfig().getFile();
		if(itemOutputFile != null && itemOutputFile.length() > 0) {
			final var itemOutputPath = Paths.get(itemOutputFile);
			if(Files.exists(itemOutputPath)) {
				Loggers.ERR.warn("Items output file \"{}\" already exists", itemOutputPath);
			}
			try {
				final var itemOutput = new ItemInfoFileOutput<>(itemOutputPath);
				controller.setIoResultsOutput(itemOutput);
			} catch(final IOException e) {
				LogUtil.exception(
					Level.ERROR, e,
					"Failed to initialize the item output, the processed items info won't be "
						+ "persisted"
				);
			}
		}

		controller.start();
	}

	@Override
	public String getTypeName() {
		return TYPE;
	}

	@Override
	protected LoadStepBase copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new LinearLoadStep(baseConfig, stepConfigs);
	}
}
