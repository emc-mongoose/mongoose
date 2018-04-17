package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.metrics.BasicMetricsContext;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
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
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import com.github.akurilov.commons.io.Output;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public class LinearLoadStep
extends LoadStepBase {

	public static final String TYPE = "Load";
	private static final int ORIGIN_INDEX = 0;

	private volatile LoadGenerator<? extends Item, ? extends IoTask> generator = null;
	private volatile StorageDriver<? extends Item, ? extends IoTask> driver = null;
	private volatile LoadController<? extends Item, ? extends IoTask> controller = null;
	private volatile DataInput dataInput = null;

	public LinearLoadStep(final Config baseConfig) {
		this(baseConfig, null);
	}

	protected LinearLoadStep(final Config baseConfig, final List<Map<String, Object>> stepConfigs) {
		super(baseConfig, stepConfigs);
	}

	@Override
	protected boolean awaitLocal(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		if(controller == null) {
			throw new IllegalStateException("Load controller is null");
		}
		try {
			return controller.await(timeout, timeUnit);
		} catch(final RemoteException ignored) {
		}
		return false;
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

		final var testStepId = stepConfig.getId();
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

		final var itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final var itemFactory = ItemType.getItemFactory(itemType);
		Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");

		try {
			driver = new BasicStorageDriverBuilder<>()
				.testStepId(testStepId)
				.itemConfig(itemConfig)
				.dataInput(dataInput)
				.loadConfig(loadConfig)
				.storageConfig(storageConfig)
				.build();
		} catch(final OmgShootMyFootException e) {
			LogUtil.exception(Level.FATAL, e, "Failed to initialize the storage driver");
			throw new IllegalStateException("Failed to initialize the storage driver");
		} catch(final InterruptedException e) {
			throw new CancellationException();
		}

		try {
			generator = new BasicLoadGeneratorBuilder<>()
				.itemConfig(itemConfig)
				.loadConfig(loadConfig)
				.limitConfig(limitConfig)
				.itemType(itemType)
				.itemFactory((ItemFactory) itemFactory)
				.storageDriver(driver)
				.authConfig(storageConfig.getAuthConfig())
				.originIndex(ORIGIN_INDEX)
				.build();
		} catch(final OmgShootMyFootException e) {
			LogUtil.exception(Level.FATAL, e, "Failed to initialize the load generator");
			throw new IllegalStateException("Failed to initialize the load generator");
		}

		final var outputConfig = actualConfig.getOutputConfig();
		final var metricsConfig = outputConfig.getMetricsConfig();
		final var ioType = IoType.valueOf(actualConfig.getLoadConfig().getType().toUpperCase());
		final var itemDataSize = actualConfig.getItemConfig().getDataConfig().getSize();
		final var concurrency = actualConfig.getLoadConfig().getLimitConfig().getConcurrency();
		final var id = stepConfig.getId();
		metricsContexts.set(
			ORIGIN_INDEX,
			new BasicMetricsContext(
				id,
				ioType,
				this::actualConcurrencyLocal,
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

		final var generators = new ArrayList<LoadGenerator>();
		generators.set(ORIGIN_INDEX, generator);
		final var drivers = new ArrayList<StorageDriver>();
		drivers.set(ORIGIN_INDEX, driver);
		final var loadConfigs = new ArrayList<LoadConfig>();
		loadConfigs.set(ORIGIN_INDEX, loadConfig);
		final var outputConfigs = new ArrayList<OutputConfig>();
		outputConfigs.set(ORIGIN_INDEX, outputConfig);

		controller = new BasicLoadController(
			testStepId, generators, drivers, null, metricsContexts, loadConfigs, stepConfig,
			outputConfigs
		);

		final var itemOutputFile = itemConfig.getOutputConfig().getFile();
		if(itemOutputFile != null && itemOutputFile.length() > 0) {
			final var itemOutputPath = Paths.get(itemOutputFile);
			if(Files.exists(itemOutputPath)) {
				Loggers.ERR.warn("Items output file \"{}\" already exists", itemOutputPath);
			}
			try {
				final var itemOutput = new ItemInfoFileOutput<>(itemOutputPath);
				controller.setIoResultsOutput((Output) itemOutput);
			} catch(final IOException e) {
				LogUtil.exception(
					Level.ERROR, e,
					"Failed to initialize the item output, the processed items info won't be "
						+ "persisted"
				);
			}
		}

		try {
			controller.start();
		} catch(final RemoteException ignored) {
		}
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
	}

	protected final int actualConcurrencyLocal() {
		return driver == null ? 0 : driver.getActiveTaskCount();
	}

	@Override
	protected final void doShutdown() {
	}

	@Override
	protected void doStopLocal() {
		try {
			controller.stop();
		} catch(final RemoteException ignored) {
		}
	}

	@Override
	protected void doCloseLocal() {

		try {
			if(driver != null && driver.isClosed()) {
				driver.close();
			}
		} catch(final IOException e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to close the storage driver \"{}\"", driver.toString()
			);
		}
		driver = null;

		try {
			if(generator != null && !generator.isClosed()) {
				generator.close();
			}
		} catch(final IOException e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to close the load generator \"{}\"", generator.toString()
			);
		}
		generator = null;

		try {
			if(controller != null && !controller.isClosed()) {
				controller.close();
			}
		} catch(final IOException e) {
			LogUtil.exception(
				Level.ERROR, e, "Failed to close the load controller \"{}\"",
				controller.toString()
			);
		}
		controller = null;

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
	public String getTypeName() {
		return TYPE;
	}

	@Override
	protected LoadStepBase copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new LinearLoadStep(baseConfig, stepConfigs);
	}
}
