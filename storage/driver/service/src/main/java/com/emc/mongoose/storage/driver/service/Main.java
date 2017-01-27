package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 Created by andrey on 05.10.16.
 */
public final class Main {

	static {
		LogUtil.init();
	}

	private static final Logger LOG = LogManager.getLogger();

	public static void main(final String... args)
	throws InterruptedException, IOException {

		final Config config = ConfigParser.loadDefaultConfig();
		if(config == null) {
			throw new AssertionError();
		}
		config.apply(CliArgParser.parseArgs(config.getAliasingConfig(), args));

		try(
			final StorageDriverBuilderSvc builderSvc = new BasicStorageDriverBuilderSvc(
				config.getStorageConfig().getDriverConfig().getPort()
			)
		) {
			builderSvc.start();
			builderSvc.await();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Storage driver builder service failure");
		}
	}
}
