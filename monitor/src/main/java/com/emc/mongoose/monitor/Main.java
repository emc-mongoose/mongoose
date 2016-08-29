package com.emc.mongoose.monitor;

import com.emc.mongoose.common.Constants;
import com.emc.mongoose.model.api.StorageType;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.ItemType;
import com.emc.mongoose.model.api.load.Monitor;
import com.emc.mongoose.storage.driver.fs.BasicFileDriver;
import com.emc.mongoose.storage.driver.http.s3.HttpS3Driver;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;

import static com.emc.mongoose.common.Constants.KEY_RUN_ID;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.RunConfig;
import com.emc.mongoose.ui.config.reader.jackson.ConfigLoader;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.generator.BasicGenerator;
import com.emc.mongoose.model.api.io.task.IoTaskFactory;
import com.emc.mongoose.model.api.item.ItemFactory;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.api.load.Generator;
import com.emc.mongoose.model.impl.io.task.BasicDataIoTaskFactory;
import com.emc.mongoose.model.impl.item.BasicMutableDataItemFactory;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 11.07.16.
 */
public class Main {

	static {
		LogUtil.init();
	}

	public static void main(final String... args)
	throws IOException, InterruptedException, UserShootHisFootException, InvocationTargetException, IllegalAccessException {

		final Config config = ConfigLoader.loadDefaultConfig();
		if(config == null) {
			throw new UserShootHisFootException("Config is null");
		}
		config.apply(CliArgParser.parseArgs(args));
		
		final StorageConfig storageConfig = config.getStorageConfig();
		final StorageType storageType = StorageType.valueOf(storageConfig.getType().toUpperCase());
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
		
		final List<Driver<? extends Item, ? extends IoTask<? extends Item>>>
			drivers = new ArrayList<>();
		if(StorageType.FS.equals(storageType)) {
			log.info(Markers.MSG, "Work on the filesystem");
			if(ItemType.CONTAINER.equals(itemType)) {
				log.info(Markers.MSG, "Work on the directories");
				// TODO directory load driver
			} else {
				log.info(Markers.MSG, "Work on the files");
				drivers.add(
					new BasicFileDriver<>(runId, loadConfig, config.getIoConfig().getBufferConfig())
				);
			}
		} else if(StorageType.HTTP.equals(storageType)){
			final String apiType = storageConfig.getHttpConfig().getApi();
			log.info(Markers.MSG, "Work via HTTP using \"{}\" cloud storage API", apiType);
			if(ItemType.CONTAINER.equals(itemType)) {
				// TODO container/bucket load driver
			} else {
				switch(apiType.toLowerCase()) {
					case "s3" :
						drivers.add(
							new HttpS3Driver<>(
								runId, loadConfig, storageConfig, config.getSocketConfig()
							)
						);
						break;
				}
			}
		} else {
			throw new UserShootHisFootException("Unsupported storage type");
		}
		log.info(Markers.MSG, "Load drivers initialized");
		
		final ItemFactory<? extends Item> itemFactory;
		if(ItemType.CONTAINER.equals(itemType)) {
			// TODO container item factory
			itemFactory = null;
			log.info(Markers.MSG, "Work on the container items");
		} else {
			itemFactory = new BasicMutableDataItemFactory();
			log.info(Markers.MSG, "Work on the mutable data items");
		}
		
		final IoTaskFactory<? extends Item, ? extends IoTask<? extends Item>> ioTaskFactory;
		if(ItemType.CONTAINER.equals(itemType)) {
			// TODO container I/O tasks factory
			ioTaskFactory = null;
		} else {
			ioTaskFactory = new BasicDataIoTaskFactory<>();
		}
		
		final List<Generator<? extends Item, IoTask<? extends Item>>>
			generators = new ArrayList<>();
		generators.add(
			new BasicGenerator(runId, drivers, itemFactory, ioTaskFactory, itemConfig, loadConfig)
		);
		log.info(Markers.MSG, "Load generators initialized");

		try(
			final Monitor<? extends Item, ? extends IoTask<? extends Item>>
				monitor = new BasicMonitor(runId, generators, loadConfig.getMetricsConfig())
		) {
			monitor.start();
			log.info(Markers.MSG, "Load monitor start");
			monitor.await();
			log.info(Markers.MSG, "Load monitor done");
		}

		for(final Generator generator : generators) {
			generator.close();
		}
		generators.clear();
		log.info(Markers.MSG, "Cleanup done");
	}
}
