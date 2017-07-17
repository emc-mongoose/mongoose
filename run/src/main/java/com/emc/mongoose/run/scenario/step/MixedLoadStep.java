package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.api.model.data.ContentSource;
import com.emc.mongoose.api.model.data.ContentSourceUtil;
import com.emc.mongoose.api.common.io.Output;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemInfoFileOutput;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.run.scenario.util.StorageDriverUtil;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.data.content.ContentConfig;
import com.emc.mongoose.ui.config.item.data.content.ring.RingConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

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
public class MixedLoadStep
extends StepBase {

	private final List<Map<String, Object>> nodeConfigList;
	private final List<Integer> weights;

	public MixedLoadStep(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig);

		nodeConfigList = (List<Map<String, Object>>) subTree.get(KEY_NODE_CONFIG);
		if(nodeConfigList == null || nodeConfigList.size() == 0) {
			throw new ScenarioParseException("Configuration list is empty");
		}

		localConfig.apply(
			nodeConfigList.get(0), "mixed-" + LogUtil.getDateTimeStamp() + "-" + hashCode()
		);
		weights = (List<Integer>) subTree.get(KEY_NODE_WEIGHTS);
		if(weights != null) {
			if(weights.size() != nodeConfigList.size()) {
				throw new ScenarioParseException("Weights count is not equal to sub-jobs count");
			}
		}
	}

	@Override
	protected final void invoke() {

		final StepConfig stepConfig = localConfig.getTestConfig().getStepConfig();
		final String stepId = stepConfig.getId();
		Loggers.MSG.info("Run the mixed load step \"{}\"", stepId);
		final LimitConfig localLimitConfig = stepConfig.getLimitConfig();
		
		final long t = localLimitConfig.getTime();
		final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;
		
		final int loadGeneratorCount = nodeConfigList.size();
		final Map<LoadGenerator, List<StorageDriver>> driverMap = new HashMap<>(loadGeneratorCount);
		final Int2IntMap weightMap = weights == null ?
			null : new Int2IntOpenHashMap(loadGeneratorCount);
		final Map<LoadGenerator, LoadConfig> loadConfigMap = new HashMap<>(loadGeneratorCount);
		final Map<LoadGenerator, OutputConfig> outputConfigMap = new HashMap<>(loadGeneratorCount);
		final Map<LoadGenerator, StepConfig> stepConfigMap = new HashMap<>(loadGeneratorCount);
		
		try {
			for(int i = 0; i < loadGeneratorCount; i ++) {
				
				final Config config = new Config(localConfig);
				config.apply(nodeConfigList.get(i), null);

				final ItemConfig itemConfig = config.getItemConfig();
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final ContentConfig contentConfig = dataConfig.getContentConfig();
				
				final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
				final RingConfig ringConfig = contentConfig.getRingConfig();
				final ContentSource contentSrc = ContentSourceUtil.getInstance(
					contentConfig.getFile(), contentConfig.getSeed(),
					ringConfig.getSize(), ringConfig.getCache()
				);
				
				final ItemFactory itemFactory = ItemType.getItemFactory(itemType);
				Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");

				final LoadConfig loadConfig = config.getLoadConfig();
				final OutputConfig outputConfig = config.getOutputConfig();
				final MetricsConfig metricsConfig = outputConfig.getMetricsConfig();
				final StorageConfig storageConfig = config.getStorageConfig();
				final LimitConfig limitConfig = stepConfig.getLimitConfig();

				final List<StorageDriver> drivers = new ArrayList<>();
				StorageDriverUtil.init(
					drivers, itemConfig, loadConfig, metricsConfig.getAverageConfig(),
					storageConfig, stepConfig, contentSrc
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
				if(weightMap != null) {
					weightMap.put(loadGenerator.hashCode(), (int) weights.get(i));
				}
				loadConfigMap.put(loadGenerator, loadConfig);
				outputConfigMap.put(loadGenerator, outputConfig);
				stepConfigMap.put(loadGenerator, stepConfig);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to init the content source");
		} catch(final UserShootHisFootException e) {
			LogUtil.exception(Level.WARN, e, "Failed to init the load generator");
		}
		
		try(
			final LoadController controller = new BasicLoadController(
				stepId, driverMap, weightMap, loadConfigMap, stepConfig, outputConfigMap
			)
		) {
			final String itemOutputFile = localConfig.getItemConfig().getOutputConfig().getFile();
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
			Loggers.MSG.info("Mixed load step \"{}\" started", controller.getName());
			if(controller.await(timeLimitSec, TimeUnit.SECONDS)) {
				Loggers.MSG.info("Mixed load step \"{}\" done", controller.getName());
			} else {
				Loggers.MSG.info("Mixed load step \"{}\" timeout", controller.getName());
			}
		} catch(final RemoteException e) {
			LogUtil.exception(Level.ERROR, e, "Unexpected failure");
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to open the item output file");
		} catch(final InterruptedException e) {
			Loggers.MSG.debug("Load step interrupted");
		}
	}

	@Override
	public void close()
	throws IOException {
		nodeConfigList.clear();
		if(weights != null) {
			weights.clear();
		}
	}
}
