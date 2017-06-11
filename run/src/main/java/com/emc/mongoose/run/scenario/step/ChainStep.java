package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.load.controller.BasicLoadController;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.model.item.BasicIoResultsOutputItemInput;
import com.emc.mongoose.model.item.IoResultsOutputItemInput;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemInfoFileOutput;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadController;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.run.scenario.util.StorageDriverUtil;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.OutputConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.QueueConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig.RingConfig;
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
public class ChainStep
extends StepBase {
	
	private final Config appConfig;
	private final List<Map<String, Object>> nodeConfigList;
	private final List<LoadController> loadChain;
	
	public ChainStep(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig);
		this.appConfig = appConfig;
		nodeConfigList = (List<Map<String, Object>>) subTree.get(KEY_NODE_CONFIG);
		if(nodeConfigList == null || nodeConfigList.size() == 0) {
			throw new ScenarioParseException("Configuration list is empty");
		}
		loadChain = new ArrayList<>(nodeConfigList.size());
	}
	
	@Override
	protected final void invoke() {
		final StepConfig stepConfig = localConfig.getTestConfig().getStepConfig();
		final String testStepName = stepConfig.getName();
		Loggers.MSG.info("Run the chain load step \"{}\"", testStepName);
		final LimitConfig commonLimitConfig = stepConfig.getLimitConfig();
		
		final long t = commonLimitConfig.getTime();
		final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;
		
		try {
			
			IoResultsOutputItemInput nextItemBuff = null;
			
			for(int i = 0; i < nodeConfigList.size(); i ++) {
				
				final Config config = new Config(appConfig);
				if(i > 0) {
					// add the config params from the 1st element as defaults
					config.apply(nodeConfigList.get(0));
				}
				config.apply(nodeConfigList.get(i));
				final ItemConfig itemConfig = config.getItemConfig();
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final ContentConfig contentConfig = dataConfig.getContentConfig();
				final OutputConfig outputConfig = itemConfig.getOutputConfig();
				
				final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
				final RingConfig ringConfig = contentConfig.getRingConfig();
				final ContentSource contentSrc = ContentSourceUtil.getInstance(
					contentConfig.getFile(), contentConfig.getSeed(),
					ringConfig.getSize(), ringConfig.getCache()
				);
				
				final ItemFactory itemFactory = ItemType.getItemFactory(itemType);
				Loggers.MSG.info("Work on the " + itemType.toString().toLowerCase() + " items");
				
				final LoadConfig loadConfig = config.getLoadConfig();
				final StorageConfig storageConfig = config.getStorageConfig();
				final QueueConfig queueConfig = loadConfig.getQueueConfig();

				final List<StorageDriver> drivers = new ArrayList<>();
				StorageDriverUtil.init(
					drivers, itemConfig, loadConfig, storageConfig, stepConfig, contentSrc
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
				final Map<LoadGenerator, StepConfig> stepConfigMap = new HashMap<>();
				stepConfigMap.put(loadGenerator, stepConfig);
				final LoadController loadController = new BasicLoadController(
					testStepName, driversMap, null, loadConfigMap, stepConfigMap
				);
				loadChain.add(loadController);
				
				if(i < nodeConfigList.size() - 1) {
					nextItemBuff = new BasicIoResultsOutputItemInput<>(
						queueConfig.getSize(), TimeUnit.SECONDS, outputConfig.getDelay()
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
				} finally {
					try {
						nextController.close();
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "Failed to close the step \"{}\"",
							nextController.getName()
						);
					}
				}
				timeRemainSec -= (System.currentTimeMillis() - tsStart) / 1000;
			} else {
				break;
			}
		}
	}
	
	@Override
	public void close()
	throws IOException {
		nodeConfigList.clear();
	}
}
