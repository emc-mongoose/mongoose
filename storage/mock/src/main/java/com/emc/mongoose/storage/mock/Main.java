package com.emc.mongoose.storage.mock;

import com.emc.mongoose.common.config.CommonConfig;
import com.emc.mongoose.common.config.CommonDecoder;
import com.emc.mongoose.common.config.reader.ConfigReader;
import com.emc.mongoose.storage.mock.http.Nagaina;

/**
 Created on 12.07.16.
 */
public class Main {

	public static void main(final String[] args) {
		final CommonConfig commonConfig = ConfigReader.loadConfig(new CommonDecoder());
		final Nagaina nagaina = new Nagaina(commonConfig);
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
