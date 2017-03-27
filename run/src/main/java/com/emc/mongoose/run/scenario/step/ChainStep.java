package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.load.monitor.BasicLoadMonitor;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.model.item.BasicIoResultsItemInput;
import com.emc.mongoose.model.item.IoResultsItemInput;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemInfoFileOutput;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
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

import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 09.01.17.
 */
public class ChainStep
extends StepBase {
	
	private static final Logger LOG = LogManager.getLogger();
	
	private final Config appConfig;
	private final List<Map<String, Object>> nodeConfigList;
	private final List<LoadMonitor> loadChain;
	
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
	public final void run() {
		super.run();
		
		final StepConfig stepConfig = localConfig.getTestConfig().getStepConfig();
		final String testStepName = stepConfig.getName();
		LOG.info(Markers.MSG, "Run the chain load job \"{}\"", testStepName);
		final LimitConfig commonLimitConfig = stepConfig.getLimitConfig();
		
		final long t = commonLimitConfig.getTime();
		final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;
		
		try {
			
			IoResultsItemInput nextItemBuff = null;
			
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
				final ContentSource contentSrc = ContentSourceUtil.getInstance(
					contentConfig.getFile(), contentConfig.getSeed(), contentConfig.getRingSize()
				);
				
				final ItemFactory itemFactory = ItemType.getItemFactory(itemType, contentSrc);
				LOG.info(Markers.MSG, "Work on the " + itemType.toString().toLowerCase() + " items");
				
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
				
				final LoadMonitor loadMonitor = new BasicLoadMonitor(
					testStepName, loadGenerator, drivers, loadConfig, stepConfig
				);
				loadChain.add(loadMonitor);
				
				if(i < nodeConfigList.size() - 1) {
					nextItemBuff = new BasicIoResultsItemInput<>(
						queueConfig.getSize(), TimeUnit.SECONDS, outputConfig.getDelay()
					);
					loadMonitor.setIoResultsOutput(nextItemBuff);
				} else {
					final String itemOutputFile = localConfig
						.getItemConfig().getOutputConfig().getFile();
					if(itemOutputFile != null && itemOutputFile.length() > 0) {
						final Path itemOutputPath = Paths.get(itemOutputFile);
						if(Files.exists(itemOutputPath)) {
							LOG.warn(
								Markers.ERR, "Items output file \"{}\" already exists",
								itemOutputPath
							);
						}
						// NOTE: using null as an ItemFactory
						final Output itemOutput = new ItemInfoFileOutput<>(
							itemOutputPath
						);
						loadMonitor.setIoResultsOutput(itemOutput);
					}
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to init the content source");
		} catch(final UserShootHisFootException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to init the load generator");
		}
		
		try {
			for(final LoadMonitor nextMonitor : loadChain) {
				nextMonitor.start();
			}
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Unexpected failure");
		}
		
		long timeRemainSec = timeLimitSec;
		long tsStart;
		for(final LoadMonitor nextMonitor : loadChain) {
			if(timeRemainSec > 0) {
				tsStart = System.currentTimeMillis();
				try {
					if(nextMonitor.await(timeRemainSec, TimeUnit.SECONDS)) {
						LOG.info(Markers.MSG, "Load monitor \"{}\" done", nextMonitor.getName());
					} else {
						LOG.info(Markers.MSG, "Load monitor \"{}\" timeout", nextMonitor.getName());
					}
				} catch(final InterruptedException e) {
					LOG.debug(Markers.MSG, "Load job interrupted");
					break;
				} catch(final RemoteException e) {
					throw new AssertionError(e);
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
		for(final LoadMonitor nextLoadMonitor : loadChain) {
			nextLoadMonitor.close();
		}
	}
}
