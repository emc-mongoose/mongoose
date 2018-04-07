package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.metrics.BasicMetricsContext;
import com.emc.mongoose.api.metrics.AggregatingMetricsContext;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
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
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
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
import java.rmi.RemoteException;
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
		try {
			return controller.await(timeout, timeUnit);
		} catch(final RemoteException ignored) {
		}
		return false;
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
			testStepId, driverByGenerator, null, metricsByIoType, loadConfigMap, stepConfig,
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

		try {
			controller.start();
		} catch(final RemoteException ignored) {
		}
	}

	@Override
	protected void init() {

		final String autoStepId = getTypeName().toLowerCase() + "_" + LogUtil.getDateTimeStamp();
		final Config config = new Config(baseConfig);
		if(stepConfigs == null || stepConfigs.size() == 0) {
			final StepConfig stepConfig = config.getTestConfig().getStepConfig();
			if(stepConfig.getIdTmp()) {
				stepConfig.setId(autoStepId);
			}
		} else {
			stepConfigs.forEach(nextStepConfig -> config.apply(nextStepConfig, autoStepId));
		}
		actualConfig(config);

		final OutputConfig outputConfig = config.getOutputConfig();
		final MetricsConfig metricsConfig = outputConfig.getMetricsConfig();
		final IoType ioType = IoType.valueOf(
			config.getLoadConfig().getType().toUpperCase()
		);
		final int ioTypeCode = ioType.ordinal();
		final SizeInBytes itemDataSize = config.getItemConfig().getDataConfig().getSize();
		final int concurrency = config.getLoadConfig().getLimitConfig().getConcurrency();
		final StepConfig stepConfig = config.getTestConfig().getStepConfig();
		final String id = stepConfig.getId();
		if(isDistributed()) {
			final int nodeCount = stepConfig.getNodeConfig().getAddrs().size();
			metricsByIoType.put(
				ioTypeCode,
				new AggregatingMetricsContext(
					id,
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
					this::getLastMetricsSnapshot
				)
			);
		} else {
			metricsByIoType.put(
				ioTypeCode,
				new BasicMetricsContext(
					id,
					ioType,
					this::actualConcurrency,
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
	protected StepBase copyInstance(final List<Map<String, Object>> stepConfigs) {
		return new LoadStep(baseConfig, stepConfigs);
	}
}
