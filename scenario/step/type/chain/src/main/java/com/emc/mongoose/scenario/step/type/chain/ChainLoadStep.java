package com.emc.mongoose.scenario.step.type.chain;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.DelayedTransferConvertBuffer;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemInfoFileOutput;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.item.TransferConvertBuffer;
import com.emc.mongoose.scenario.step.type.LoadController;
import com.emc.mongoose.scenario.step.type.LoadGenerator;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.scenario.step.type.BasicLoadController;
import com.emc.mongoose.scenario.step.type.BasicLoadGeneratorBuilder;
import com.emc.mongoose.scenario.step.type.LoadGeneratorBuilder;
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
import java.util.concurrent.TimeUnit;

public class ChainLoadStep
extends LoadStepBase  {

	public static final String TYPE = "ChainLoad";

	public ChainLoadStep(final Config baseConfig) {
		super(baseConfig, null);
	}

	public ChainLoadStep(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		super(baseConfig, stepConfigs);
	}

	@Override
	protected ChainLoadStep copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new ChainLoadStep(baseConfig, stepConfigs);
	}

	@Override
	protected void init() {

		final String autoStepId = "chain_" + LogUtil.getDateTimeStamp();
		final Config config = new Config(baseConfig);
		final StepConfig stepConfig = config.getTestConfig().getStepConfig();
		if(stepConfig.getIdTmp()) {
			stepConfig.setId(autoStepId);
		}
		actualConfig(config);
		final int subStepCount = stepConfigs.size();
		TransferConvertBuffer<? extends Item, ? extends IoTask<? extends Item>> nextItemBuff = null;

		for(int originIndex = 0; originIndex < subStepCount; originIndex ++) {

			final Config subConfig = new Config(config);
			subConfig.apply(stepConfigs.get(originIndex), id());
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
							final LoadGeneratorBuilder generatorBuilder = new BasicLoadGeneratorBuilder<>()
								.itemConfig(itemConfig)
								.loadConfig(loadConfig)
								.limitConfig(limitConfig)
								.itemType(itemType)
								.itemFactory((ItemFactory) itemFactory)
								.storageDriver(driver)
								.authConfig(storageConfig.getAuthConfig())
								.originIndex(originIndex);
							if(rateLimit > 0) {
								generatorBuilder.rateThrottle(new RateThrottle(rateLimit));
							}
							if(nextItemBuff != null) {
								generatorBuilder.itemInput(nextItemBuff);
							}
							final LoadGenerator generator = generatorBuilder.build();
							generators.add(generator);

							final LoadController controller = new BasicLoadController<>(
								testStepId, generator, driver, metricsContexts.get(originIndex),
								limitConfig,
								outputConfig.getMetricsConfig().getTraceConfig().getPersist(),
								loadConfig.getBatchConfig().getSize(),
								loadConfig.getGeneratorConfig().getRecycleConfig().getLimit()
							);
							controllers.add(controller);

							if(originIndex < subStepCount - 1) {
								nextItemBuff = new DelayedTransferConvertBuffer<>(
									storageConfig.getDriverConfig().getQueueConfig().getOutput(),
									TimeUnit.SECONDS, itemConfig.getOutputConfig().getDelay()
								);
								controller.ioResultsOutput(nextItemBuff);
							} else {
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

	@Override
	protected void doCloseLocal() {

		final int n = Math.max(Math.max(generators.size(), drivers.size()), controllers.size());

		for(int i = 0; i < n; i ++) {
			if(i < generators.size()) {
				try {
					generators.get(i).close();
				} catch(final IOException e) {
					LogUtil.exception(
						Level.ERROR, e, "Failed to close the load generator \"{}\"",
						generators.get(i).toString()
					);
				}
			} else {
				Loggers.ERR.warn(
					"The count of load generators is " + generators.size() + " but expected " + n
				);
			}
			if(i < drivers.size()) {
				try {
					drivers.get(i).close();
				} catch(final IOException e) {
					LogUtil.exception(
						Level.ERROR, e, "Failed to close the storage driver \"{}\"",
						drivers.get(i).toString()
					);
				}
			} else {
				Loggers.ERR.warn(
					"The count of storage drivers is " + drivers.size() + " but expected " + n
				);
			}
			if(i < controllers.size()) {
				try {
					controllers.get(i).close();
				} catch(final IOException e) {
					LogUtil.exception(
						Level.ERROR, e, "Failed to close the load controller \"{}\"",
						controllers.get(i).toString()
					);
				}
			} else {
				Loggers.ERR.warn(
					"The count of load controllers is " + controllers.size() + " but expected " + n
				);
			}
		}

		generators.clear();
		drivers.clear();
		controllers.clear();
	}

	@Override
	public String getTypeName() {
		return TYPE;
	}
}
