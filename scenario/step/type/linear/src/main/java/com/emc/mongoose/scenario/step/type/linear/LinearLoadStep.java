package com.emc.mongoose.scenario.step.type.linear;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemInfoFileOutput;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.scenario.step.type.LoadStepBase;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.data.input.InputConfig;
import com.emc.mongoose.ui.config.item.data.input.layer.LayerConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.test.TestConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.system.SizeInBytes;

import com.github.akurilov.concurrent.throttle.RateThrottle;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class LinearLoadStep
extends LoadStepBase {

	public static final String TYPE = "Load";

	public LinearLoadStep(final Config baseConfig) {
		this(baseConfig, null);
	}

	public LinearLoadStep(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		super(baseConfig, stepConfigs);
	}

	@Override
	public String getTypeName() {
		return TYPE;
	}

	@Override
	protected LoadStepBase copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new LinearLoadStep(baseConfig, stepConfigs);
	}

	@Override
	protected void init() {

		final String autoStepId = "linear_" + LogUtil.getDateTimeStamp();
		final Config config = new Config(baseConfig);
		final TestConfig testConfig = config.getTestConfig();
		final StepConfig stepConfig = testConfig.getStepConfig();
		if(stepConfigs == null || stepConfigs.size() == 0) {
			if(stepConfig.getIdTmp()) {
				stepConfig.setId(autoStepId);
			}
		} else {
			stepConfigs.forEach(nextStepConfig -> config.apply(nextStepConfig, autoStepId));
		}
		actualConfig(config);

		final LoadConfig loadConfig = config.getLoadConfig();
		final IoType ioType = IoType.valueOf(loadConfig.getType().toUpperCase());
		final int concurrency = loadConfig.getLimitConfig().getConcurrency();
		final OutputConfig outputConfig = config.getOutputConfig();
		final MetricsConfig metricsConfig = outputConfig.getMetricsConfig();
		final SizeInBytes itemDataSize = config.getItemConfig().getDataConfig().getSize();

		if(distributedFlag) {
			initDistributedMetrics(
				0, ioType, concurrency, stepConfig.getNodeConfig().getAddrs().size(),
				metricsConfig, itemDataSize, outputConfig.getColor()
			);
		} else {

			initLocalMetrics(
				ioType, concurrency, metricsConfig, itemDataSize, outputConfig.getColor()
			);

			final ItemConfig itemConfig = config.getItemConfig();
			final StorageConfig storageConfig = config.getStorageConfig();
			final DataConfig dataConfig = itemConfig.getDataConfig();
			final InputConfig dataInputConfig = dataConfig.getInputConfig();
			final LimitConfig limitConfig = stepConfig.getLimitConfig();
			final LayerConfig dataLayerConfig = dataInputConfig.getLayerConfig();

			final String testStepId = stepConfig.getId();

			try {

				final DataInput dataInput = DataInput.getInstance(
					dataInputConfig.getFile(), dataInputConfig.getSeed(), dataLayerConfig.getSize(),
					dataLayerConfig.getCache()
				);

				try {

					final StorageDriver driver = new BasicStorageDriverBuilder<>()
						.testStepId(testStepId)
						.itemConfig(itemConfig)
						.dataInput(dataInput)
						.loadConfig(loadConfig)
						.storageConfig(storageConfig)
						.build();
					drivers.add(driver);

					final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
					final ItemFactory<? extends Item> itemFactory = ItemType.getItemFactory(itemType);
					final double rateLimit = loadConfig.getLimitConfig().getRate();

					try {
						final LoadGenerator generator = new BasicLoadGeneratorBuilder<>()
							.itemConfig(itemConfig)
							.loadConfig(loadConfig)
							.limitConfig(limitConfig)
							.itemType(itemType)
							.itemFactory((ItemFactory) itemFactory)
							.storageDriver(driver)
							.authConfig(storageConfig.getAuthConfig())
							.originIndex(0)
							.rateThrottle(rateLimit > 0 ? new RateThrottle(rateLimit) : null)
							.weightThrottle(null)
							.build();
						generators.add(generator);

						final LoadController controller = new BasicLoadController<>(
							testStepId, generator, driver, metricsContexts.get(0), limitConfig,
							outputConfig.getMetricsConfig().getTraceConfig().getPersist(),
							loadConfig.getBatchConfig().getSize(),
							loadConfig.getGeneratorConfig().getRecycleConfig().getLimit()
						);
						controllers.add(controller);

						final String itemOutputFile = itemConfig.getOutputConfig().getFile();
						if(itemOutputFile != null && itemOutputFile.length() > 0) {
							final Path itemOutputPath = Paths.get(itemOutputFile);
							if(Files.exists(itemOutputPath)) {
								if(distributedFlag) {
									Files.delete(itemOutputPath);
								} else {
									Loggers.ERR.warn(
										"Items output file \"{}\" already exists", itemOutputPath
									);
								}
							}
							try {
								final Output<? extends Item> itemOutput = new ItemInfoFileOutput<>(
									itemOutputPath
								);
								controller.ioResultsOutput(itemOutput);
							} catch(final IOException e) {
								LogUtil.exception(
									Level.ERROR, e,
									"Failed to initialize the item output, the processed items " +
										"info won't be persisted"
								);
							}
						}
					} catch(final OmgShootMyFootException e) {
						throw new IllegalStateException(
							"Failed to initialize the load generator", e
						);
					}
				} catch(final OmgShootMyFootException e) {
					throw new IllegalStateException("Failed to initialize the storage driver", e);
				} catch(final InterruptedException e) {
					throw new CancellationException();
				}
			} catch(final IOException e) {
				throw new IllegalStateException("Failed to initialize the data input", e);
			}
		}
	}
}
