package com.emc.mongoose.run.scenario.job;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.load.monitor.BasicLoadMonitor;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemInfoFileOutput;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.run.scenario.util.StorageDriverUtil;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;

import com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.LimitConfig;
import com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.MetricsConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 08.11.16.
 */
public class MixedLoadJob
extends JobBase {

	private static final Logger LOG = LogManager.getLogger();

	private final Config appConfig;
	private final List<Map<String, Object>> nodeConfigList;
	private final List<Integer> weights;

	public MixedLoadJob(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig);
		this.appConfig = appConfig;

		nodeConfigList = (List<Map<String, Object>>) subTree.get(KEY_NODE_CONFIG);
		if(nodeConfigList == null || nodeConfigList.size() == 0) {
			throw new ScenarioParseException("Configuration list is empty");
		}
		localConfig.apply(nodeConfigList.get(0));

		weights = (List<Integer>) subTree.get(KEY_NODE_WEIGHTS);
		if(weights != null) {
			if(weights.size() != nodeConfigList.size()) {
				throw new ScenarioParseException("Weights count is not equal to sub-jobs count");
			}
		}
	}

	@Override
	public final void run() {
		super.run();

		final StepConfig localStepConfig = localConfig.getTestConfig().getStepConfig();
		final String jobName = localStepConfig.getName();
		LOG.info(Markers.MSG, "Run the mixed load job \"{}\"", jobName);
		final LimitConfig localLimitConfig = localStepConfig.getLimitConfig();
		
		final long t = localLimitConfig.getTime();
		final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;
		final boolean remoteDriversFlag = localConfig
			.getStorageConfig().getDriverConfig().getRemote();
		
		final int loadGeneratorCount = nodeConfigList.size();
		final Map<LoadGenerator, List<StorageDriver>> driverMap = new HashMap<>(loadGeneratorCount);
		final Int2IntMap weightMap = weights == null ?
			null : new Int2IntOpenHashMap(loadGeneratorCount);
		final Map<LoadGenerator, LoadConfig> loadConfigMap = new HashMap<>(loadGeneratorCount);
		final Map<LoadGenerator, StepConfig> stepConfigMap = new HashMap<>(loadGeneratorCount);
		
		try {
			for(int i = 0; i < loadGeneratorCount; i ++) {
				
				final Config config = new Config(appConfig);
				if(i > 0) {
					// add the config params from the 1st element as defaults
					config.apply(nodeConfigList.get(0));
				}
				config.apply(nodeConfigList.get(i));
				final ItemConfig itemConfig = config.getItemConfig();
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final ContentConfig contentConfig = dataConfig.getContentConfig();
				
				final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
				final ContentSource contentSrc = ContentSourceUtil.getInstance(
					contentConfig.getFile(), contentConfig.getSeed(), contentConfig.getRingSize()
				);
				
				final ItemFactory itemFactory = ItemType.getItemFactory(itemType, contentSrc);
				LOG.info(Markers.MSG, "Work on the " + itemType.toString().toLowerCase() + " items");

				final LoadConfig loadConfig = config.getLoadConfig();
				final StorageConfig storageConfig = config.getStorageConfig();
				final StepConfig stepConfig = config.getTestConfig().getStepConfig();
				final LimitConfig limitConfig = stepConfig.getLimitConfig();

				final List<StorageDriver> drivers = new ArrayList<>();
				StorageDriverUtil.init(
					drivers, itemConfig, loadConfig, storageConfig, stepConfig, contentSrc
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
				stepConfigMap.put(loadGenerator, stepConfig);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to init the content source");
		} catch(final UserShootHisFootException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to init the load generator");
		}
		
		try(
			final LoadMonitor monitor = new BasicLoadMonitor(
				jobName, driverMap, weightMap, loadConfigMap, stepConfigMap
			)
		) {
			final String itemOutputFile = localConfig.getItemConfig().getOutputConfig().getFile();
			if(itemOutputFile != null && itemOutputFile.length() > 0) {
				final Path itemOutputPath = Paths.get(itemOutputFile);
				if(Files.exists(itemOutputPath)) {
					LOG.warn(
						Markers.ERR, "Items output file \"{}\" already exists", itemOutputPath
					);
				}
				// NOTE: using null as an ItemFactory
				final Output itemOutput = new ItemInfoFileOutput<>(itemOutputPath);
				monitor.setIoResultsOutput(itemOutput);
			}
			monitor.start();
			if(monitor.await(timeLimitSec, TimeUnit.SECONDS)) {
				LOG.info(Markers.MSG, "Load monitor done");
			} else {
				LOG.info(Markers.MSG, "Load monitor timeout");
			}
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to open the item output file");
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Load monitor interrupted");
		}
	}

	@Override
	public void close()
	throws IOException {
		nodeConfigList.clear();
		weights.clear();
	}
}
