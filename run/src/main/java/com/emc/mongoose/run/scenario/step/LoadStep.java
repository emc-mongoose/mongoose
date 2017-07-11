package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemInfoFileOutput;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadController;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.run.scenario.util.StorageDriverUtil;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.data.content.ContentConfig;
import com.emc.mongoose.ui.config.item.data.content.ring.RingConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 02.02.16.
 */
public final class LoadStep
extends StepBase {
	
	private final boolean preconditionFlag;

	public LoadStep(final Config config) {
		this(config, Collections.EMPTY_MAP, false);
	}
	
	public LoadStep(
		final Config appConfig, final Map<String, Object> subTree, final boolean preconditionFlag
	) {
		super(appConfig);
		final Map<String, Object> nodeConfig = (Map<String, Object>) subTree.get(KEY_NODE_CONFIG);
		localConfig.apply(nodeConfig, "load-" + LogUtil.getDateTimeStamp() + "-" + hashCode());
		this.preconditionFlag = preconditionFlag;
	}
	
	@Override
	protected final void invoke() {

		final StepConfig stepConfig = localConfig.getTestConfig().getStepConfig();
		final String stepId = stepConfig.getId();
		Loggers.MSG.info("Run the load step \"{}\"", stepId);
		final LoadConfig loadConfig = localConfig.getLoadConfig();
		final LimitConfig limitConfig = stepConfig.getLimitConfig();
		final OutputConfig outputConfig = localConfig.getOutputConfig();
		final MetricsConfig metricsConfig = outputConfig.getMetricsConfig();
		final AverageConfig avgMetricsConfig = metricsConfig.getAverageConfig();
		if(preconditionFlag) {
			avgMetricsConfig.setPersist(false);
			metricsConfig.getSummaryConfig().setPersist(false);
			metricsConfig.getSummaryConfig().setPerfDbResultsFile(false);
			metricsConfig.getTraceConfig().setPersist(false);
		}
		final ItemConfig itemConfig = localConfig.getItemConfig();
		final DataConfig dataConfig = itemConfig.getDataConfig();
		final ContentConfig contentConfig = dataConfig.getContentConfig();
		final StorageConfig storageConfig = localConfig.getStorageConfig();
		final RingConfig ringConfig = contentConfig.getRingConfig();
		
		final ContentSource contentSrc;
		try {
			contentSrc = ContentSourceUtil.getInstance(
				contentConfig.getFile(), contentConfig.getSeed(), ringConfig.getSize(),
				ringConfig.getCache()
			);
		} catch(final IOException e) {
			throw new RuntimeException(e);
		}
		
		final List<StorageDriver> drivers = new ArrayList<>();
		StorageDriverUtil.init(
			drivers, itemConfig, loadConfig, avgMetricsConfig, storageConfig, stepConfig, contentSrc
		);

		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final ItemFactory itemFactory = ItemType.getItemFactory(itemType);
		Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");

		final LoadGenerator loadGenerator;
		try {
			loadGenerator = new BasicLoadGeneratorBuilder<>()
				.setItemConfig(itemConfig)
				.setLoadConfig(loadConfig)
				.setLimitConfig(limitConfig)
				.setItemType(itemType)
				.setItemFactory(itemFactory)
				.setStorageDrivers(drivers)
				.setAuthConfig(storageConfig.getAuthConfig())
				.build();
		} catch(final UserShootHisFootException e) {
			throw new RuntimeException(e);
		}
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
		final Map<LoadGenerator, LoadConfig> loadConfigMap = new HashMap<>();
		loadConfigMap.put(loadGenerator, loadConfig);
		final Map<LoadGenerator, OutputConfig> outputConfigMap = new HashMap<>();
		outputConfigMap.put(loadGenerator, outputConfig);
		try(
			final LoadController controller = new BasicLoadController(
				stepId, driversMap, null, loadConfigMap, stepConfig, outputConfigMap
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
		} catch(final RemoteException e) {
			LogUtil.exception(Level.ERROR, e, "Unexpected failure");
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to open the item output file");
		} catch(final InterruptedException e) {
			Loggers.MSG.debug("Load step \"{}\" interrupted", stepId);
		}
	}
	
	@Override
	public final String toString() {
		return "singleLoadStepContainer#" + hashCode();
	}
	
	@Override
	public final void close()
	throws IOException {
	}
}
