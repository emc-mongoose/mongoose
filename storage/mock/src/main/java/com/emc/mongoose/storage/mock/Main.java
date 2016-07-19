package com.emc.mongoose.storage.mock;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.JacksonConfigLoader;
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
		final Config config = JacksonConfigLoader.loadDefaultConfig();
		final Nagaina nagaina = new Nagaina(config);
		nagaina.start();
		System.out.println("Nagaina started");
		try {
			System.out.println("Nagaina will await");
			nagaina.await();
		} catch(final InterruptedException e) {
			System.out.println("Nagaina was interrupted");
		}
		nagaina.shutdown();
		System.out.println("Nagaina shutdown");
	}

}
