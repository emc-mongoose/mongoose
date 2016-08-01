package com.emc.mongoose.monitor;

import com.emc.mongoose.model.api.StorageType;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.ItemType;
import com.emc.mongoose.model.api.load.Monitor;
import com.emc.mongoose.model.impl.io.task.BasicIoTask;
import com.emc.mongoose.model.impl.item.BasicItem;
import com.emc.mongoose.storage.driver.fs.BasicFileDriver;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
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

import java.io.IOException;
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
	throws IOException, InterruptedException, UserShootHisFootException {

		final Config config = ConfigLoader.loadDefaultConfig();
		if(config == null) {
			throw new UserShootHisFootException("Config is null");
		}
		
		final StorageType storageType = StorageType.valueOf(
			config.getStorageConfig().getType().toUpperCase()
		);
		final ItemConfig itemConfig = config.getItemConfig();
		final ItemType itemType = ItemType.valueOf(itemConfig.getType().toUpperCase());
		final LoadConfig loadConfig = config.getLoadConfig();
		
		final List<Driver<? extends Item, ? extends IoTask<? extends Item>>>
			drivers = new ArrayList<>();
		if(StorageType.FS.equals(storageType)) {
			if(ItemType.CONTAINER.equals(itemType)) {
				// TODO directory load driver
			} else {
				drivers.add(
					new BasicFileDriver<>(loadConfig, config.getIoConfig().getBufferConfig())
				);
			}
		} else {
			if(ItemType.CONTAINER.equals(itemType)) {
				// TODO container/bucket load driver
			} else {
				// TODO HTTP storage data object load driver
			}
		}
		
		final ItemFactory<? extends Item> itemFactory;
		if(ItemType.CONTAINER.equals(itemType)) {
			// TODO container item factory
			itemFactory = null;
		} else {
			itemFactory = new BasicMutableDataItemFactory();
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
			new BasicGenerator(drivers, itemFactory, ioTaskFactory, itemConfig, loadConfig)
		);

		try(
			final Monitor<? extends Item, ? extends IoTask<? extends Item>>
				monitor = new BasicMonitor("test", generators, loadConfig.getMetricsConfig())
		) {
			monitor.start();
			monitor.await();
		}

		for(final Generator generator : generators) {
			generator.close();
		}
		generators.clear();
	}
}
