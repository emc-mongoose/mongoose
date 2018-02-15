package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemInfoFileOutput;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.data.input.InputConfig;
import com.emc.mongoose.ui.config.item.data.input.layer.LayerConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.test.TestConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public class LoadStep
extends StepBase {

	public static final String TYPE = "Load";

	private volatile LoadGenerator generator = null;
	private volatile StorageDriver driver = null;
	private volatile LoadController controller = null;
	private volatile DataInput dataInput = null;

	public LoadStep(final Config baseConfig) {
		this(baseConfig, null);
	}

	protected LoadStep(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		super(baseConfig, stepConfigs);
	}

	@Override
	protected boolean awaitLocal(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		if(controller == null) {
			throw new IllegalStateException("Load controller is null");
		}
		return controller.await(timeout, timeUnit);
	}

	@Override
	protected void doStartLocal(final Config actualConfig) {

		final ItemConfig itemConfig = actualConfig.getItemConfig();
		final LoadConfig loadConfig = actualConfig.getLoadConfig();
		final StorageConfig storageConfig = actualConfig.getStorageConfig();
		final TestConfig testConfig = actualConfig.getTestConfig();
		final OutputConfig outputConfig = actualConfig.getOutputConfig();

		final DataConfig dataConfig = itemConfig.getDataConfig();
		final StepConfig stepConfig = testConfig.getStepConfig();

		final InputConfig dataInputConfig = dataConfig.getInputConfig();
		final LimitConfig limitConfig = stepConfig.getLimitConfig();

		final LayerConfig dataLayerConfig = dataInputConfig.getLayerConfig();

		final String testStepId = stepConfig.getId();
		Loggers.MSG.info("Run the load step \"{}\"", testStepId);

		try {
			dataInput = DataInput.getInstance(
				dataInputConfig.getFile(), dataInputConfig.getSeed(), dataLayerConfig.getSize(),
				dataLayerConfig.getCache()
			);
		} catch(final IOException e) {
			LogUtil.exception(Level.FATAL, e, "Failed to initialize the data input");
			throw new IllegalStateException("Failed to initialize the data input");
		}

		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final ItemFactory itemFactory = ItemType.getItemFactory(itemType);
		Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");

		try {
			driver = new BasicStorageDriverBuilder<>()
				.setTestStepName(testStepId)
				.setItemConfig(itemConfig)
				.setContentSource(dataInput)
				.setLoadConfig(loadConfig)
				.setStorageConfig(storageConfig)
				.build();
		} catch(final OmgShootMyFootException e) {
			LogUtil.exception(Level.FATAL, e, "Failed to initialize the storage driver");
			throw new IllegalStateException("Failed to initialize the storage driver");
		} catch(final InterruptedException e) {
			throw new CancellationException();
		}

		try {
			generator = new BasicLoadGeneratorBuilder<>()
				.setItemConfig(itemConfig)
				.setLoadConfig(loadConfig)
				.setLimitConfig(limitConfig)
				.setItemType(itemType)
				.setItemFactory(itemFactory)
				.setStorageDriver(driver)
				.setAuthConfig(storageConfig.getAuthConfig())
				.build();
		} catch(final OmgShootMyFootException e) {
			LogUtil.exception(Level.FATAL, e, "Failed to initialize the load generator");
			throw new IllegalStateException("Failed to initialize the load generator");
		}

		final Map<LoadGenerator, StorageDriver> driverByGenerator = new HashMap<>();
		driverByGenerator.put(generator, driver);
		final Map<LoadGenerator, SizeInBytes> itemDataSizes = new HashMap<>();
		itemDataSizes.put(generator, dataConfig.getSize());
		final Map<LoadGenerator, LoadConfig> loadConfigMap = new HashMap<>();
		loadConfigMap.put(generator, loadConfig);
		final Map<LoadGenerator, OutputConfig> outputConfigMap = new HashMap<>();
		outputConfigMap.put(generator, outputConfig);

		controller = new BasicLoadController(
			testStepId, driverByGenerator, null, itemDataSizes, loadConfigMap, stepConfig,
			outputConfigMap
		);

		final String itemOutputFile = itemConfig.getOutputConfig().getFile();
		if(itemOutputFile != null && itemOutputFile.length() > 0) {
			final Path itemOutputPath = Paths.get(itemOutputFile);
			if(Files.exists(itemOutputPath)) {
				Loggers.ERR.warn("Items output file \"{}\" already exists", itemOutputPath);
			}
			try {
				final Output itemOutput = new ItemInfoFileOutput<>(itemOutputPath);
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
	protected void doStopLocal() {
		controller.interrupt();
	}

	@Override
	protected void doCloseLocal() {

		if(driver != null && driver.isClosed()) {
			try {
				driver.close();
			} catch(final IOException e) {
				LogUtil.exception(
					Level.ERROR, e, "Failed to close the storage driver \"{}\"", driver.toString()
				);
			}
			driver = null;
		}

		if(generator != null && !generator.isClosed()) {
			try {
				generator.close();
			} catch(final IOException e) {
				LogUtil.exception(
					Level.ERROR, e, "Failed to close the load generator \"{}\"",
					generator.toString()
				);
			}
			generator = null;
		}

		if(controller != null && !controller.isClosed()) {
			try {
				controller.close();
			} catch(final IOException e) {
				LogUtil.exception(
					Level.ERROR, e, "Failed to close the load controller \"{}\"",
					controller.toString()
				);
			}
			controller = null;
		}

		if(dataInput != null) {
			try {
				dataInput.close();
			} catch(final IOException e) {
				LogUtil.exception(
					Level.ERROR, e, "Failed to close the data input \"{}\"", dataInput.toString()
				);
			}
			dataInput = null;
		}
	}

	@Override
	protected String getTypeName() {
		return TYPE;
	}

	@Override
	protected StepBase copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new LoadStep(baseConfig, stepConfigs);
	}
}
