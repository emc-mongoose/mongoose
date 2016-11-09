package com.emc.mongoose.run.scenario;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.load.generator.BasicLoadGenerator;
import com.emc.mongoose.load.monitor.BasicLoadMonitor;
import com.emc.mongoose.load.monitor.BasicLoadMonitorSvc;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.model.io.Output;
import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.io.task.BasicMutableDataIoTaskBuilder;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.BasicMutableDataItemFactory;
import com.emc.mongoose.model.item.CsvFileItemOutput;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.load.LoadType;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.Config.ItemConfig;
import com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
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
public class MixedLoadJob
extends JobBase {

	private static final Logger LOG = LogManager.getLogger();

	private final Config appConfig;
	private final List<Map<String, Object>> nodeConfigList;
	private final List<Integer> weights;

	public MixedLoadJob(final Config appConfig, final Map<String, Object> subTree) {
		super(appConfig);
		this.appConfig = appConfig;

		nodeConfigList = (List<Map<String, Object>>) subTree.get(KEY_NODE_CONFIG);
		if(nodeConfigList != null && nodeConfigList.size() > 0) {
		} else {
			throw new IllegalArgumentException("Configuration list is empty");
		}
		localConfig.apply(nodeConfigList.get(0));

		weights = (List<Integer>) subTree.get(KEY_NODE_WEIGHTS);
		if(weights != null) {
			if(weights.size() != nodeConfigList.size()) {
				throw new IllegalArgumentException("Weights count is not equal to sub-jobs count");
			}
		}
	}

	@Override
	public final void run() {
		super.run();

		final LoadConfig localLoadConfig = localConfig.getLoadConfig();
		final String jobName = localLoadConfig.getJobConfig().getName();
		final LimitConfig limitConfig = localLoadConfig.getLimitConfig();
		
		final long t = limitConfig.getTime();
		final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;
		final boolean remoteDriversFlag = localConfig.getStorageConfig().getDriverConfig().getRemote();
		
		final int loadGeneratorCount = nodeConfigList.size();
		final Map<LoadGenerator, List<StorageDriver>> driverMap = new HashMap<>(loadGeneratorCount);
		final Object2IntMap<LoadGenerator> weightMap = weights == null ?
			null : new Object2IntOpenHashMap<>(loadGeneratorCount);
		final Map<LoadGenerator, LoadConfig> loadConfigMap = new HashMap<>(loadGeneratorCount);
		
		try {
			for(int i = 0; i < loadGeneratorCount; i ++) {
				
				final Config config = new Config(appConfig);
				config.apply(nodeConfigList.get(i));
				final ItemConfig itemConfig = config.getItemConfig();
				final DataConfig dataConfig = itemConfig.getDataConfig();
				final ContentConfig contentConfig = dataConfig.getContentConfig();
				
				final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
				final ContentSource contentSrc = ContentSourceUtil.getInstance(
					contentConfig.getFile(), contentConfig.getSeed(), contentConfig.getRingSize()
				);
				
				final ItemFactory itemFactory;
				if(ItemType.DATA.equals(itemType)) {
					itemFactory = new BasicMutableDataItemFactory(contentSrc);
					LOG.info(Markers.MSG, "Work on the mutable data items");
				} else {
					// TODO path item factory
					itemFactory = null;
					LOG.info(Markers.MSG, "Work on the path items");
				}
				
				final LoadConfig loadConfig = config.getLoadConfig();
				final IoTaskBuilder ioTaskBuilder;
				if(ItemType.PATH.equals(itemType)) {
					// TODO path I/O tasks factory
					ioTaskBuilder = new BasicIoTaskBuilder();
				} else {
					ioTaskBuilder = new BasicMutableDataIoTaskBuilder<>()
						.setRangesConfig(dataConfig.getRangesConfig());
				}
				ioTaskBuilder.setSrcPath(itemConfig.getInputConfig().getPath());
				ioTaskBuilder.setIoType(LoadType.valueOf(loadConfig.getType().toUpperCase()));
				
				final LoadGenerator loadGenerator = new BasicLoadGenerator(
					itemFactory, ioTaskBuilder, itemConfig, loadConfig
				);
				
				final List<StorageDriver> drivers = new ArrayList<>();
				final StorageConfig storageConfig = config.getStorageConfig();
				final DriverConfig driverConfig = storageConfig.getDriverConfig();
				final SocketConfig socketConfig = config.getSocketConfig();
				if(remoteDriversFlag) {
					final List<String> driverSvcAddrs = driverConfig.getAddrs();
					for(final String driverSvcAddr : driverSvcAddrs) {
						final StorageDriverBuilderSvc driverBuilderSvc = ServiceUtil.resolve(
							driverSvcAddr, StorageDriverBuilderSvc.SVC_NAME
						);
						LOG.info(
							Markers.MSG, "Connected the service \"{}\" @ {}",
							StorageDriverBuilderSvc.SVC_NAME, driverSvcAddr
						);
						if(driverBuilderSvc == null) {
							LOG.warn(
								Markers.ERR,
								"Failed to resolve the storage driver builder service @ {}",
								driverSvcAddr
							);
							continue;
						}
						final String driverSvcName = driverBuilderSvc
							.setJobName(jobName)
							.setContentSource(contentSrc)
							.setItemConfig(itemConfig)
							.setLoadConfig(loadConfig)
							.setSocketConfig(socketConfig)
							.setStorageConfig(storageConfig)
							.buildRemotely();
						final StorageDriverSvc driverSvc = ServiceUtil.resolve(
							driverSvcAddr, driverSvcName
						);
						LOG.info(
							Markers.MSG, "Connected the service \"{}\" @ {}", driverSvcName,
							driverSvcAddr
						);
						if(driverSvc != null) {
							drivers.add(driverSvc);
						} else {
							LOG.warn(
								Markers.ERR, "Failed to resolve the storage driver service @ {}",
								driverSvcAddr
							);
						}
					}
				} else {
					drivers.add(
						new BasicStorageDriverBuilder<>()
							.setJobName(jobName)
							.setContentSource(contentSrc)
							.setItemConfig(itemConfig)
							.setLoadConfig(loadConfig)
							.setSocketConfig(socketConfig)
							.setStorageConfig(storageConfig)
							.build()
					);
				}
				
				driverMap.put(loadGenerator, drivers);
				if(weightMap != null) {
					weightMap.put(loadGenerator, weights.get(i));
				}
				loadConfigMap.put(loadGenerator, loadConfig);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to init the content source");
		} catch(final UserShootHisFootException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to init the load generator");
		}
		
		try(
			final LoadMonitor monitor = remoteDriversFlag ?
				new BasicLoadMonitorSvc(jobName, driverMap, loadConfigMap, weightMap) :
				new BasicLoadMonitor(jobName, driverMap, loadConfigMap, weightMap)
		) {
			final String itemOutputFile = localConfig.getItemConfig().getOutputConfig().getFile();
			if(itemOutputFile != null && itemOutputFile.length() > 0) {
				final Path itemOutputPath = Paths.get(itemOutputFile);
				final Output itemOutput = new CsvFileItemOutput(itemOutputPath, null); // NOTE: using null as an ItemFactory
				monitor.setItemOutput(itemOutput);
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
	}
}
