package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.common.io.Output;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.item.BasicChainTransferBuffer;
import com.emc.mongoose.api.model.item.ChainTransferBuffer;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemInfoFileOutput;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.model.load.LoadController;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.run.scenario.util.StorageDriverUtil;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.data.input.InputConfig;
import com.emc.mongoose.ui.config.item.data.input.layer.LayerConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.load.queue.QueueConfig;
import com.emc.mongoose.ui.config.output.OutputConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 09.01.17.
 */
public class ChainLoadStep
extends StepBase {
	
	private final List<Map<String, Object>> nodeConfigList;
	private final List<LoadController> loadChain;
	
	public ChainLoadStep(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig);
		nodeConfigList = (List<Map<String, Object>>) subTree.get(KEY_NODE_CONFIG);
		if(nodeConfigList == null || nodeConfigList.size() == 0) {
			throw new ScenarioParseException("Configuration list is empty");
		}
		// add the config params from the 1st element as defaults
		this.localConfig.apply(
			nodeConfigList.get(0), "chain-" + LogUtil.getDateTimeStamp() + "-" + hashCode()
		);

		loadChain = new ArrayList<>(nodeConfigList.size());
	}
	
	@Override
	protected final void invoke() {

		StepConfig stepConfig = localConfig.getTestConfig().getStepConfig();
		final String testStepName = stepConfig.getId();
		Loggers.MSG.info("Run the chain load step \"{}\"", testStepName);
		final LimitConfig commonLimitConfig = stepConfig.getLimitConfig();
		
		final long t = commonLimitConfig.getTime();
		final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;
		
		try {
			
			ChainTransferBuffer nextItemBuff = null;
			
			for(int i = 0; i < nodeConfigList.size(); i ++) {
				
				final Config config = new Config(localConfig);
				config.apply(
					nodeConfigList.get(i),
					"chain-load-" + LogUtil.getDateTimeStamp() + "-" + config.hashCode()
				);

				stepConfig = config.getTestConfig().getStepConfig();
				final ItemConfig itemConfig = config.getItemConfig();
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final InputConfig dataInputConfig = dataConfig.getInputConfig();
				final com.emc.mongoose.ui.config.item.output.OutputConfig
					itemOutputConfig = itemConfig.getOutputConfig();
				
				final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
				final LayerConfig dataLayerConfig = dataInputConfig.getLayerConfig();
				final DataInput dataInput = DataInput.getInstance(
					dataInputConfig.getFile(), dataInputConfig.getSeed(),
					dataLayerConfig.getSize(), dataLayerConfig.getCache()
				);
				
				final ItemFactory itemFactory = ItemType.getItemFactory(itemType);
				Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");
				
				final LoadConfig loadConfig = config.getLoadConfig();
				final OutputConfig outputConfig = config.getOutputConfig();
				final StorageConfig storageConfig = config.getStorageConfig();
				final QueueConfig queueConfig = loadConfig.getQueueConfig();
				final MetricsConfig metricsConfig = config.getOutputConfig().getMetricsConfig();

				final List<StorageDriver> drivers = new ArrayList<>();
				StorageDriverUtil.init(
					drivers, itemConfig, loadConfig, metricsConfig.getAverageConfig(),
					storageConfig, stepConfig, dataInput
				);
				
				final LoadGenerator loadGenerator;
				if(nextItemBuff == null) {
					loadGenerator = new BasicLoadGeneratorBuilder<>()
						.setItemConfig(itemConfig)
						.setItemFactory(itemFactory)
						.setItemType(itemType)
						.setLoadConfig(loadConfig)
						.setLimitConfig(commonLimitConfig)
						.setStorageDrivers(drivers)
						.setAuthConfig(storageConfig.getAuthConfig())
						.build();
				} else {
					loadGenerator = new BasicLoadGeneratorBuilder<>()
						.setItemConfig(itemConfig)
						.setItemFactory(itemFactory)
						.setItemType(itemType)
						.setLoadConfig(loadConfig)
						.setLimitConfig(commonLimitConfig)
						.setStorageDrivers(drivers)
						.setAuthConfig(storageConfig.getAuthConfig())
						.setItemInput(nextItemBuff)
						.build();
				}
				
				final Map<LoadGenerator, List<StorageDriver>> driversMap = new HashMap<>();
				driversMap.put(loadGenerator, drivers);
				final Map<LoadGenerator, LoadConfig> loadConfigMap = new HashMap<>();
				loadConfigMap.put(loadGenerator, loadConfig);
				final Map<LoadGenerator, OutputConfig> outputConfigMap = new HashMap<>();
				outputConfigMap.put(loadGenerator, outputConfig);
				final LoadController loadController = new BasicLoadController(
					stepConfig.getId(), driversMap, null, loadConfigMap, stepConfig,
					outputConfigMap
				);
				loadChain.add(loadController);
				
				if(i < nodeConfigList.size() - 1) {
					nextItemBuff = new BasicChainTransferBuffer<>(
						queueConfig.getSize(), TimeUnit.SECONDS, itemOutputConfig.getDelay()
					);
					loadController.setIoResultsOutput(nextItemBuff);
				} else {
					final String itemOutputFile = localConfig
						.getItemConfig().getOutputConfig().getFile();
					if(itemOutputFile != null && itemOutputFile.length() > 0) {
						final Path itemOutputPath = Paths.get(itemOutputFile);
						if(Files.exists(itemOutputPath)) {
							Loggers.ERR.warn(
								"Items output file \"{}\" already exists", itemOutputPath
							);
						}
						// NOTE: using null as an ItemFactory
						final Output itemOutput = new ItemInfoFileOutput<>(
							itemOutputPath
						);
						loadController.setIoResultsOutput(itemOutput);
					}
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to init the content source");
		} catch(final UserShootHisFootException e) {
			LogUtil.exception(Level.WARN, e, "Failed to init the load generator");
		}
		
		try {
			for(final LoadController nextController : loadChain) {
				nextController.start();
				Loggers.MSG.info("Load step \"{}\" started", nextController.getName());
			}
		} catch(final RemoteException e) {
			LogUtil.exception(Level.WARN, e, "Unexpected failure");
		}
		
		long timeRemainSec = timeLimitSec;
		long tsStart;
		for(final LoadController nextController : loadChain) {
			if(timeRemainSec > 0) {
				tsStart = System.currentTimeMillis();
				try {
					if(nextController.await(timeRemainSec, TimeUnit.SECONDS)) {
						Loggers.MSG.info("Load step \"{}\" done", nextController.getName());
					} else {
						Loggers.MSG.info("Load step \"{}\" timeout", nextController.getName());
					}
				} catch(final InterruptedException e) {
					Loggers.MSG.debug("Load step interrupted");
					break;
				} catch(final RemoteException e) {
					throw new AssertionError(e);
				}
				timeRemainSec -= (System.currentTimeMillis() - tsStart) / 1000;
			} else {
				break;
			}
		}

		for(final LoadController nextController : loadChain) {
			try {
				nextController.close();
			} catch(final IOException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to close the step \"{}\"",  nextController.getName()
				);
			}
		}
	}
	
	@Override
	public void close()
	throws IOException {
		nodeConfigList.clear();
	}
}
