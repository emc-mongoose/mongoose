package com.emc.mongoose.monitor;

import com.emc.mongoose.model.api.StorageType;
import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.io.Output;
import com.emc.mongoose.model.api.item.ItemType;
import com.emc.mongoose.model.api.load.Monitor;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.model.impl.item.CsvFileItemOutput;
import com.emc.mongoose.storage.driver.fs.BasicFileDriver;
import com.emc.mongoose.storage.driver.http.s3.HttpS3Driver;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.common.Constants.KEY_RUN_ID;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.RunConfig;

import com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
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
import java.nio.file.Path;
import java.nio.file.Paths;
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
		
		final List<Driver> drivers = new ArrayList<>();
		if(StorageType.FS.equals(storageType)) {
			log.info(Markers.MSG, "Work on the filesystem");
			if(ItemType.CONTAINER.equals(itemType)) {
				log.info(Markers.MSG, "Work on the directories");
				// TODO directory load driver
			} else {
				log.info(Markers.MSG, "Work on the files");
				drivers.add(
					new BasicFileDriver<>(
						runId, storageConfig.getAuthConfig(), loadConfig,
						itemConfig.getInputConfig().getContainer(),
						config.getIoConfig().getBufferConfig().getSize()
					)
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
								runId, loadConfig, storageConfig,
								itemConfig.getInputConfig().getContainer(), config.getSocketConfig()
							)
						);
						break;
				}
			}
		} else {
			throw new UserShootHisFootException("Unsupported storage type");
		}
		log.info(Markers.MSG, "Load drivers initialized");
		
		final IoTaskFactory ioTaskFactory;
		if(ItemType.CONTAINER.equals(itemType)) {
			// TODO container I/O tasks factory
			ioTaskFactory = null;
		} else {
			ioTaskFactory = new BasicDataIoTaskFactory<>();
		}

		final LimitConfig limitConfig = loadConfig.getLimitConfig();
		final long timeLimitSec = limitConfig.getTime();
		final ContentConfig contentConfig = itemConfig.getDataConfig().getContentConfig();
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
			
			final List<Generator> generators = new ArrayList<>();
			
			generators.add(
				new BasicGenerator(
					runId, drivers, itemFactory, ioTaskFactory, itemConfig, loadConfig
				)
			);
			log.info(Markers.MSG, "Load generators initialized");
			
			try(final Monitor monitor = new BasicMonitor(runId, generators, loadConfig)) {
				
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
		} catch(final Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}
