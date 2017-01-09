package com.emc.mongoose.run.scenario;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.load.generator.BasicLoadGeneratorBuilder;
import com.emc.mongoose.load.monitor.BasicLoadMonitor;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.BasicMutableDataItemFactory;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemInfoFileOutput;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
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
public class ChainJob
extends JobBase {
	
	private static final Logger LOG = LogManager.getLogger();
	
	private final Config appConfig;
	private final List<Map<String, Object>> nodeConfigList;
	private final List<LoadMonitor> loadChain;
	
	public ChainJob(final Config appConfig, final Map<String, Object> subTree)
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
		
		final LoadConfig localLoadConfig = localConfig.getLoadConfig();
		final String jobName = localLoadConfig.getJobConfig().getName();
		LOG.info(Markers.MSG, "Run the mixed load job \"{}\"", jobName);
		final LimitConfig limitConfig = localLoadConfig.getLimitConfig();
		
		final long t = limitConfig.getTime();
		final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;
		final boolean remoteDriversFlag = localConfig
			.getStorageConfig().getDriverConfig().getRemote();
		
		try {
			
			Output<IoResult> nextItemOutput = null;
			
			for(int i = 0; i < nodeConfigList.size(); i ++) {
				
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
							.setItemConfig(itemConfig)
							.setLoadConfig(loadConfig)
							.setSocketConfig(socketConfig)
							.setStorageConfig(storageConfig)
							.build()
					);
				}
				
				final LoadGenerator loadGenerator;
				if(nextItemOutput == null) {
					loadGenerator = new BasicLoadGeneratorBuilder<>()
						.setItemConfig(itemConfig)
						.setItemFactory(itemFactory)
						.setItemType(itemType)
						.setLoadConfig(loadConfig)
						.setStorageDrivers(drivers)
						.build();
				} else {
					loadGenerator = new BasicLoadGeneratorBuilder<>()
						.setItemConfig(itemConfig)
						.setItemFactory(itemFactory)
						.setItemType(itemType)
						.setLoadConfig(loadConfig)
						.setStorageDrivers(drivers)
						.setItemInput(/*TODO*/null)
						.build();
				}
				
				final LoadMonitor loadMonitor = new BasicLoadMonitor(
					jobName, loadGenerator, drivers, loadConfig
				);
				loadChain.add(loadMonitor);
				
				if(i < nodeConfigList.size() - 1) {
					/*TODO*/
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
						final Output<IoResult> itemOutput = new ItemInfoFileOutput<>(
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
					assert false;
					LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
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
