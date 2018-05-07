package com.emc.mongoose.scenario.step.type.weighted;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemInfoFileOutput;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.scenario.step.type.LoadController;
import com.emc.mongoose.scenario.step.type.LoadGenerator;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.scenario.step.type.BasicLoadController;
import com.emc.mongoose.scenario.step.type.BasicLoadGeneratorBuilder;
import com.emc.mongoose.scenario.step.type.LoadStepBase;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.config.Config;
import com.emc.mongoose.config.item.ItemConfig;
import com.emc.mongoose.config.item.data.DataConfig;
import com.emc.mongoose.config.item.data.input.InputConfig;
import com.emc.mongoose.config.item.data.input.layer.LayerConfig;
import com.emc.mongoose.config.load.LoadConfig;
import com.emc.mongoose.config.output.OutputConfig;
import com.emc.mongoose.config.output.metrics.MetricsConfig;
import com.emc.mongoose.config.storage.StorageConfig;
import com.emc.mongoose.config.scenario.step.StepConfig;
import com.emc.mongoose.config.scenario.step.limit.LimitConfig;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.system.SizeInBytes;

import com.github.akurilov.concurrent.throttle.IndexThrottle;
import com.github.akurilov.concurrent.throttle.RateThrottle;
import com.github.akurilov.concurrent.throttle.SequentialWeightsThrottle;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class WeightedLoadStep
extends LoadStepBase {

	public static final String TYPE = "WeightedLoad";

	public WeightedLoadStep(
		final Config baseConfig, final ClassLoader clsLoader,
		final List<Map<String, Object>> overrides
	) {
		super(baseConfig, clsLoader, overrides);
	}

	@Override
	public String getTypeName() {
		return TYPE;
	}

	@Override
	protected WeightedLoadStep copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new WeightedLoadStep(baseConfig, clsLoader, stepConfigs);
	}

	@Override
	protected void init() {

		final String autoStepId = "weighted_" + LogUtil.getDateTimeStamp();
		final Config config = new Config(baseConfig);
		final StepConfig stepConfig = config.getScenarioConfig().getStepConfig();
		if(stepConfig.getIdTmp()) {
			stepConfig.setId(autoStepId);
		}
		actualConfig(config);

		final int subStepCount = stepConfigs.size();

		// 1st pass: determine the weights map
		final int[] weights = new int[subStepCount];
		final List<Config> subConfigs = new ArrayList<>(subStepCount);
		for(int i = 0; i < subStepCount; i ++) {
			final Config subConfig = new Config(config);
			subConfig.apply(stepConfigs.get(i), id());
			subConfigs.add(subConfig);
			final int weight = subConfig.getLoadConfig().getGeneratorConfig().getWeight();
			weights[i] = weight;
		}

		final IndexThrottle weightThrottle = new SequentialWeightsThrottle(weights);

		// 2nd pass: initialize the sub steps
		for(int originIndex = 0; originIndex < subStepCount; originIndex ++) {

			final Config subConfig = subConfigs.get(originIndex);
			final LoadConfig loadConfig = subConfig.getLoadConfig();
			final IoType ioType = IoType.valueOf(loadConfig.getType().toUpperCase());
			final int concurrency = loadConfig.getLimitConfig().getConcurrency();
			final OutputConfig outputConfig = subConfig.getOutputConfig();
			final MetricsConfig metricsConfig = outputConfig.getMetricsConfig();
			final SizeInBytes itemDataSize = subConfig.getItemConfig().getDataConfig().getSize();

			if(distributedFlag) {
				initDistributedMetrics(
					originIndex, ioType, concurrency, stepConfig.getNodeConfig().getAddrs().size(),
					metricsConfig, itemDataSize, outputConfig.getColor()
				);
			} else {

				initLocalMetrics(
					ioType, concurrency, metricsConfig, itemDataSize, outputConfig.getColor()
				);

				final ItemConfig itemConfig = subConfig.getItemConfig();
				final StorageConfig storageConfig = subConfig.getStorageConfig();
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final InputConfig dataInputConfig = dataConfig.getInputConfig();
				final LimitConfig limitConfig = stepConfig.getLimitConfig();
				final LayerConfig dataLayerConfig = dataInputConfig.getLayerConfig();

				final String testStepId = stepConfig.getId();

				try {

					final DataInput dataInput = DataInput.getInstance(
						dataInputConfig.getFile(), dataInputConfig.getSeed(),
						dataLayerConfig.getSize(), dataLayerConfig.getCache()
					);

					try {

						final StorageDriver driver = new BasicStorageDriverBuilder<>()
							.classLoader(clsLoader)
							.testStepId(testStepId)
							.itemConfig(itemConfig)
							.dataInput(dataInput)
							.loadConfig(loadConfig)
							.storageConfig(storageConfig)
							.build();
						drivers.add(driver);

						final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
						final ItemFactory<? extends Item>
							itemFactory = ItemType.getItemFactory(itemType);
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
								.originIndex(originIndex)
								.rateThrottle(rateLimit > 0 ? new RateThrottle(rateLimit) : null)
								.weightThrottle(weightThrottle)
								.build();
							generators.add(generator);

							final LoadController controller = new BasicLoadController<>(
								testStepId, generator, driver, metricsContexts.get(originIndex),
								limitConfig,
								outputConfig.getMetricsConfig().getTraceConfig().getPersist(),
								loadConfig.getBatchConfig().getSize(),
								loadConfig.getGeneratorConfig().getRecycleConfig().getLimit()
							);
							controllers.add(controller);

							final String itemOutputFile = itemConfig.getOutputConfig().getFile();
							if(itemOutputFile != null && itemOutputFile.length() > 0) {
								final Path itemOutputPath = Paths.get(itemOutputFile);
								if(Files.exists(itemOutputPath)) {
									Loggers.ERR.warn(
										"Items output file \"{}\" already exists", itemOutputPath
									);
								}
								try {
									final Output<? extends Item>
										itemOutput = new ItemInfoFileOutput<>(itemOutputPath);
									controller.ioResultsOutput(itemOutput);
								} catch(final IOException e) {
									LogUtil.exception(
										Level.ERROR, e,
										"Failed to initialize the item output, the processed " +
											"items info won't be persisted"
									);
								}
							}
						} catch(final OmgShootMyFootException e) {
							throw new IllegalStateException(
								"Failed to initialize the load generator", e
							);
						}
					} catch(final OmgShootMyFootException e) {
						throw new IllegalStateException(
							"Failed to initialize the storage driver", e
						);
					} catch(final InterruptedException e) {
						throw new CancellationException();
					}
				} catch(final IOException e) {
					throw new IllegalStateException("Failed to initialize the data input", e);
				}
			}
		}
	}
}
