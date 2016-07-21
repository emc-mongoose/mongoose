package com.emc.mongoose.storage.mock;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigLoader;
import com.emc.mongoose.storage.mock.impl.http.Nagaina;
import com.emc.mongoose.ui.log.LogUtil;

import java.io.IOException;

/**
 Created on 12.07.16.
 */
public class Main {

	static {
		LogUtil.init();
	}

	public static void main(final String... args)
	throws IOException {
		final Config config = ConfigLoader.loadDefaultConfig();
		if (config == null) {
			throw new IllegalStateException();
		}
		final Config.StorageConfig storageConfig = config.getStorageConfig();
		final Config.LoadConfig.MetricsConfig metricsConfig =
			config.getLoadConfig().getMetricsConfig();
		final Config.ItemConfig itemConfig = config.getItemConfig();
		try(final Nagaina nagaina = new Nagaina(storageConfig, metricsConfig, itemConfig)) {
			nagaina.start();
			System.out.println("Nagaina started");
			try {
				nagaina.await();
			} catch(final InterruptedException e) {
				System.out.println("Nagaina was interrupted");
			}
		}
	}

}
