package com.emc.mongoose.monitor;

import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.io.DataIoTask;
import com.emc.mongoose.common.io.factory.BasicDataIoTaskFactory;
import com.emc.mongoose.common.io.factory.IoTaskFactory;
import com.emc.mongoose.common.item.BasicDataItem;
import com.emc.mongoose.common.item.DataItem;
import com.emc.mongoose.common.item.factory.BasicDataItemFactory;
import com.emc.mongoose.common.item.factory.ItemFactory;
import com.emc.mongoose.common.load.Driver;
import com.emc.mongoose.common.load.Generator;
import com.emc.mongoose.common.load.Monitor;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.generator.BasicGenerator;
import com.emc.mongoose.storage.driver.BasicDriver;

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
	throws IOException {

		final int generatorCount = Runtime.getRuntime().availableProcessors();
		final List<Generator<I, O>> generators = new ArrayList<>(generatorCount);
		for(int i = 0; i < generatorCount; i ++) {
			final List<Driver<I, O>> drivers = new ArrayList<>(2);
			drivers.add(new BasicDriver<>());
			drivers.add(new BasicDriver<>());
			generators.add(
				new BasicGenerator<>(
					drivers, LoadType.CREATE,
					(ItemFactory) new BasicDataItemFactory(),
					(IoTaskFactory) new BasicDataIoTaskFactory<BasicDataItem>()
				)
			);
		}

		try(final Monitor<I, O> monitor = new BasicMonitor<>(generators)) {
			monitor.start();
			monitor.await();
		} catch(final Throwable e) {
			e.printStackTrace(System.out);
		}

		for(final Generator<I, O> generator : generators) {
			generator.close();
		}
		generators.clear();
	}
}
