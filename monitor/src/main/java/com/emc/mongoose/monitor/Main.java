package com.emc.mongoose.monitor;

import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.ItemType;
import com.emc.mongoose.model.api.load.LoadMonitor;
import com.emc.mongoose.model.api.load.StorageDriverSvc;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.model.impl.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.impl.io.task.BasicMutableDataIoTaskBuilder;
import com.emc.mongoose.model.impl.item.CsvFileItemOutput;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.storage.driver.fs.FileStorageDriver;
import com.emc.mongoose.storage.driver.http.s3.HttpS3StorageDriver;
import com.emc.mongoose.storage.driver.service.BasicStorageDriverConfigFactory;
import com.emc.mongoose.storage.driver.service.CommonStorageDriverConfigFactory;
import com.emc.mongoose.storage.driver.service.StorageDriverFactorySvc;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.common.Constants.KEY_RUN_ID;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.RunConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.InputConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;

import com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig.HttpConfig.Api;
import com.emc.mongoose.ui.config.Config.StorageConfig.StorageType;
import com.emc.mongoose.ui.config.reader.jackson.ConfigLoader;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.generator.BasicLoadGenerator;
import com.emc.mongoose.model.api.io.task.IoTaskBuilder;
import com.emc.mongoose.model.api.item.ItemFactory;
import com.emc.mongoose.model.api.load.StorageDriver;
import com.emc.mongoose.model.api.load.LoadGenerator;
import com.emc.mongoose.model.impl.item.BasicMutableDataItemFactory;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
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

	public static void main(final String... args)
	throws IOException, InterruptedException, UserShootHisFootException, InvocationTargetException,
		IllegalAccessException {

		final Config config = ConfigLoader.loadDefaultConfig();
		if(config == null) {
			throw new UserShootHisFootException("Config is null");
		}
		config.apply(CliArgParser.parseArgs(args));
		
		final StorageConfig storageConfig = config.getStorageConfig();
		final StorageType storageType = storageConfig.getType();
		final ItemConfig itemConfig = config.getItemConfig();
		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final LoadConfig loadConfig = config.getLoadConfig();
		final RunConfig runConfig = config.getRunConfig();
		final InputConfig inputConfig = itemConfig.getInputConfig();
		final Api apiType = storageConfig.getHttpConfig().getApi();

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
		final List<StorageDriver> drivers = new ArrayList<>();
		final DriverConfig driverConfig = storageConfig.getDriverConfig();
		if(!driverConfig.getRemote()) {
			if(StorageType.FS.equals(storageType)) {
				log.info(Markers.MSG, "Work on the filesystem");
				if(ItemType.CONTAINER.equals(itemType)) {
					log.info(Markers.MSG, "Work on the directories");
					// TODO directory load driver
				} else {
					log.info(Markers.MSG, "Work on the files");
					drivers.add(
						new FileStorageDriver<>(
							runId, loadConfig, inputConfig.getContainer(), storageConfig,
							itemConfig.getDataConfig().getVerify(),
							config.getIoConfig().getBufferConfig().getSize()
					));
				}
			} else if(StorageType.HTTP.equals(storageType)) {

				log.info(Markers.MSG, "Work via HTTP using \"{}\" cloud storage API", apiType);
				if(ItemType.CONTAINER.equals(itemType)) {
					// TODO container/bucket load driver
				} else {
					switch(apiType) {
						case S3:
							drivers.add(new HttpS3StorageDriver<>(
								runId, loadConfig, inputConfig.getContainer(),
								storageConfig, itemConfig.getDataConfig().getVerify(),
								config.getSocketConfig()
							));
							break;
					}
				}
			} else {
				throw new UserShootHisFootException("Unsupported storage type");
			}
		} else {
			final List<String> driverConfigAddrs = driverConfig.getAddrs();
			for (final String addrs: driverConfigAddrs) {
				try {
					final StorageDriverFactorySvc<
						? extends Item, ? extends IoTask<Item>, CommonStorageDriverConfigFactory
						> driverFactorySvc = ServiceUtil.getSvc(
						addrs, StorageDriverFactorySvc.SVC_NAME
					);
					final CommonStorageDriverConfigFactory defaultSdConfigFactory =
						new BasicStorageDriverConfigFactory(
							storageType, runId, loadConfig, inputConfig.getContainer(),
							storageConfig, itemConfig.getDataConfig().getVerify(),
							config.getSocketConfig()
							);
					final String driverSvcName = driverFactorySvc.create(defaultSdConfigFactory);
					final StorageDriverSvc driverSvc = ServiceUtil.getSvc(addrs, driverSvcName);
					drivers.add(driverSvc);
				} catch(final NotBoundException e) {
					log.error(
						Markers.ERR, "Storage driver factory service is not bound on {}", addrs
					);
				}
			}
		}
		log.info(Markers.MSG, "Load drivers initialized");
		
		final DataConfig dataConfig = itemConfig.getDataConfig();
		final IoTaskBuilder ioTaskBuilder;
		if(ItemType.CONTAINER.equals(itemType)) {
			// TODO container I/O tasks factory
			ioTaskBuilder = new BasicIoTaskBuilder();
		} else {
			ioTaskBuilder = new BasicMutableDataIoTaskBuilder<>()
				.setRangesConfig(dataConfig.getRanges());
		}
		ioTaskBuilder.setIoType(LoadType.valueOf(loadConfig.getType().toUpperCase()));

		final LimitConfig limitConfig = loadConfig.getLimitConfig();
		final long t = limitConfig.getTime();
		final long timeLimitSec = t > 0 ? t : Long.MAX_VALUE;
		final ContentConfig contentConfig = dataConfig.getContentConfig();
		try(
			final ContentSource contentSrc = ContentSourceUtil.getInstance(
				contentConfig.getFile(), contentConfig.getSeed(), contentConfig.getRingSize()
			)
		) {
			
			final ItemFactory itemFactory;
			if(ItemType.CONTAINER.equals(itemType)) {
				// TODO container item factory
				itemFactory = null;
				log.info(Markers.MSG, "Work on the container items");
			} else {
				itemFactory = new BasicMutableDataItemFactory(contentSrc);
				log.info(Markers.MSG, "Work on the mutable data items");
			}
			
			final List<LoadGenerator> generators = new ArrayList<>();
			
			generators.add(
				new BasicLoadGenerator(
					runId, drivers, itemFactory, ioTaskBuilder, itemConfig, loadConfig
				)
			);
			log.info(Markers.MSG, "Load generators initialized");
			
			try(
				final LoadMonitor monitor = new BasicLoadMonitor(
					runId, generators, drivers, loadConfig
				)
			) {
				
				final String itemOutputFile = itemConfig.getOutputConfig().getFile();
				if(itemOutputFile != null && itemOutputFile.length() > 0) {
					final Path itemOutputPath = Paths.get(itemOutputFile);
					final Output itemOutput = new CsvFileItemOutput(itemOutputPath, itemFactory);
					monitor.setItemOutput(itemOutput);
				}
				
				monitor.start();
				log.info(Markers.MSG, "Load monitor start");
				if(monitor.await(timeLimitSec, TimeUnit.SECONDS)) {
					log.info(Markers.MSG, "Load monitor done");
				} else {
					log.info(Markers.MSG, "Load monitor timeout");
				}
			}
			ServiceUtil.shutdown();
		} catch(final Throwable throwable) {
			throwable.printStackTrace(System.err);
		}
	}
}
