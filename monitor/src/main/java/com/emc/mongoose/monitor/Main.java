package com.emc.mongoose.monitor;

import com.emc.mongoose.storage.driver.fs.BasicFileDriver;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.ui.config.reader.jackson.JacksonConfigLoader;
import com.emc.mongoose.common.exception.UserShootItsFootException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.generator.BasicGenerator;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.IoTaskFactory;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.ItemFactory;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.api.load.Generator;
import com.emc.mongoose.model.api.load.Monitor;
import com.emc.mongoose.model.impl.io.task.BasicDataIoTaskFactory;
import com.emc.mongoose.model.impl.item.BasicDataItem;
import com.emc.mongoose.model.impl.item.BasicDataItemFactory;

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

	public static <I extends DataItem, O extends DataIoTask<I>> void main(final String... args)
	throws IOException, InterruptedException, UserShootItsFootException {

		final Config config = JacksonConfigLoader.loadDefaultConfig();

		final int generatorCount = Runtime.getRuntime().availableProcessors();
		final List<Generator<I, O>> generators = new ArrayList<>(generatorCount);
		for(int i = 0; i < generatorCount; i ++) {
			final List<Driver<I, O>> drivers = new ArrayList<>(2);
			drivers.add(new BasicFileDriver<>(config.getLoadConfig(), config.getIoConfig().getBufferConfig()));
			drivers.add(new BasicFileDriver<>(config.getLoadConfig(), config.getIoConfig().getBufferConfig()));
			generators.add(
				new BasicGenerator<>(
					drivers, LoadType.CREATE, (ItemFactory<I>) new BasicDataItemFactory(),
					(IoTaskFactory<I, O>) new BasicDataIoTaskFactory<BasicDataItem>()
				)
			);
		}

		try(
			final Monitor<I, O> monitor = new BasicMonitor<>(
				"test", generators, config.getLoadConfig().getMetricsConfig()
			)
		) {
			monitor.start();
			monitor.await();
		}

		for(final Generator<I, O> generator : generators) {
			generator.close();
		}
		generators.clear();
	}
}
