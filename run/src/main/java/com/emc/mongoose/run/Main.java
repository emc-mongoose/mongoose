package com.emc.mongoose.run;

import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.load.monitor.BasicLoadMonitor;
import com.emc.mongoose.load.monitor.BasicLoadMonitorSvc;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.Output;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.io.task.BasicMutableDataIoTaskBuilder;
import com.emc.mongoose.model.item.CsvFileItemOutput;
import com.emc.mongoose.model.load.LoadType;
import com.emc.mongoose.storage.driver.builder.BasicStorageDriverBuilder;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.common.Constants.KEY_RUN_ID;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.RunConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
import com.emc.mongoose.ui.config.reader.jackson.ConfigLoader;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.load.generator.BasicLoadGenerator;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.item.BasicMutableDataItemFactory;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 11.07.16.
 */
public class Main {

	static {
		LogUtil.init();
	}

	@SuppressWarnings("unchecked")
	public static void main(final String... args)
	throws IOException, InterruptedException, UserShootHisFootException, InvocationTargetException,
		IllegalAccessException {

		final Config config = ConfigLoader.loadDefaultConfig();
		if(config == null) {
			throw new UserShootHisFootException("Config is null");
		}
		config.apply(CliArgParser.parseArgs(args));
		
		final StorageConfig storageConfig = config.getStorageConfig();
		final ItemConfig itemConfig = config.getItemConfig();
		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final LoadConfig loadConfig = config.getLoadConfig();
		final RunConfig runConfig = config.getRunConfig();

		String runId = runConfig.getId();
		if(runId == null) {
			runId = ThreadContext.get(KEY_RUN_ID);
			runConfig.setId(runId);
		} else {
			ThreadContext.put(KEY_RUN_ID, runId);
		}
		if(runId == null) {
			throw new IllegalStateException("Run id is not set");
		}
		
		final Logger log = LogManager.getLogger();
		log.info(Markers.MSG, "Configuration loaded");

		final DataConfig dataConfig = itemConfig.getDataConfig();
		final ContentConfig contentConfig = dataConfig.getContentConfig();
		try(
			final ContentSource contentSrc = ContentSourceUtil.getInstance(
				contentConfig.getFile(), contentConfig.getSeed(), contentConfig.getRingSize()
			)
		) {
			final List<StorageDriver> drivers = new ArrayList<>();
			final DriverConfig driverConfig = storageConfig.getDriverConfig();
			final boolean remoteDriversFlag = driverConfig.getRemote();
			if(remoteDriversFlag) {
				final List<String> driverSvcAddrs = driverConfig.getAddrs();
				for(final String driverSvcAddr : driverSvcAddrs) {
					final StorageDriverBuilderSvc driverBuilderSvc = ServiceUtil.resolve(
						driverSvcAddr, StorageDriverBuilderSvc.SVC_NAME
					);
					log.info(
						Markers.MSG, "Connected the service \"{}\" @ {}",
						StorageDriverBuilderSvc.SVC_NAME, driverSvcAddr
					);
					if(driverBuilderSvc == null) {
						log.warn(
							Markers.ERR,
							"Failed to resolve the storage driver builder service @ {}",
							driverSvcAddr
						);
						continue;
					}
					try {
						final String driverSvcName = driverBuilderSvc
							.setRunId(runId)
							.setContentSource(contentSrc)
							.setItemConfig(itemConfig)
							.setLoadConfig(loadConfig)
							.setSocketConfig(config.getSocketConfig())
							.setStorageConfig(storageConfig)
							.buildRemotely();
						final StorageDriverSvc driverSvc = ServiceUtil.resolve(
							driverSvcAddr, driverSvcName
						);
						log.info(
							Markers.MSG, "Connected the service \"{}\" @ {}", driverSvcName,
							driverSvcAddr
						);
						if(driverSvc != null) {
							drivers.add(driverSvc);
						} else {
							log.warn(
								Markers.ERR, "Failed to resolve the storage driver service @ {}",
								driverSvcAddr
							);
						}
					} catch(final RemoteException e) {
						LogUtil.exception(log, Level.WARN, e, "Looks like network failure");
					}
				}
			} else {
				drivers.add(
					new BasicStorageDriverBuilder<>()
						.setRunId(runId)
						.setContentSource(contentSrc)
						.setItemConfig(itemConfig)
						.setLoadConfig(loadConfig)
						.setSocketConfig(config.getSocketConfig())
						.setStorageConfig(storageConfig)
						.build()
				);
			}
			log.info(Markers.MSG, "Load drivers initialized");

			final IoTaskBuilder ioTaskBuilder;
			if(ItemType.PATH.equals(itemType)) {
				// TODO path I/O tasks factory
				ioTaskBuilder = new BasicIoTaskBuilder();
			} else {
				ioTaskBuilder = new BasicMutableDataIoTaskBuilder<>()
					.setRangesConfig(dataConfig.getRanges());
			}
			ioTaskBuilder.setSrcPath(itemConfig.getInputConfig().getPath());
			ioTaskBuilder.setIoType(LoadType.valueOf(loadConfig.getType().toUpperCase()));

			final LimitConfig limitConfig = loadConfig.getLimitConfig();
			final long t = limitConfig.getTime();
			final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;

			final ItemFactory itemFactory;
			if(ItemType.DATA.equals(itemType)) {
				itemFactory = new BasicMutableDataItemFactory(contentSrc);
				log.info(Markers.MSG, "Work on the mutable data items");
			} else {
				// TODO path item factory
				itemFactory = null;
				log.info(Markers.MSG, "Work on the path items");
			}
			
			final List<LoadGenerator> generators = new ArrayList<>();
			
			generators.add(
				new BasicLoadGenerator(
					runId, drivers, itemFactory, ioTaskBuilder, itemConfig, loadConfig
				)
			);
			log.info(Markers.MSG, "Load generators initialized");
			
			try(
				final LoadMonitor monitor = remoteDriversFlag ?
					new BasicLoadMonitorSvc(runId, generators, drivers, loadConfig) :
					new BasicLoadMonitor(runId, generators, drivers, loadConfig)
			) {
				
				final String itemOutputFile = itemConfig.getOutputConfig().getFile();
				if(itemOutputFile != null && itemOutputFile.length() > 0) {
					final Path itemOutputPath = Paths.get(itemOutputFile);
					final Output itemOutput = new CsvFileItemOutput(itemOutputPath, itemFactory);
					monitor.setItemOutput(itemOutput);
				}
				
				monitor.start();
				if(monitor.await(timeLimitSec, TimeUnit.SECONDS)) {
					log.info(Markers.MSG, "Load monitor done");
				} else {
					log.info(Markers.MSG, "Load monitor timeout");
				}
			}
		} catch(final Throwable throwable) {
			throwable.printStackTrace(System.err);
		}
	}
}
