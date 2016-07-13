package com.emc.mongoose.monitor;

import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.io.DataIoTask;
import com.emc.mongoose.common.item.BasicDataItem;
import com.emc.mongoose.common.item.DataItem;
import com.emc.mongoose.common.load.Driver;
import com.emc.mongoose.common.load.Generator;
import com.emc.mongoose.common.load.Monitor;
import com.emc.mongoose.generator.GeneratorMock;
import com.emc.mongoose.storage.driver.DriverMock;

import java.io.IOException;
import java.util.Arrays;

/**
 Created by kurila on 11.07.16.
 */
public class Main {

	public static <I extends DataItem, O extends DataIoTask<I>> void main(final String[] args)
	throws IOException, InterruptedException {
		try(
			final Driver<I, O> driver = new DriverMock<>()
		) {
			try(
				final Generator<I, O> generator = new GeneratorMock<>(
					Arrays.asList(driver), (Class<I>) BasicDataItem.class, LoadType.CREATE
				)
			) {
				try(
					final Monitor<I, O> monitor = new MonitorMock<>(Arrays.asList(generator))
				) {
					monitor.start();
					monitor.await();
				}
			}
		}
	}
}
