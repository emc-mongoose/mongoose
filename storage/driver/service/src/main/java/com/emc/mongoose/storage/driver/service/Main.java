package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.IllegalArgumentNameException;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import org.apache.logging.log4j.Level;

import java.io.IOException;

import static com.emc.mongoose.ui.cli.CliArgParser.formatCliArgsList;
import static com.emc.mongoose.ui.cli.CliArgParser.getAllCliArgs;

/**
 Created by andrey on 05.10.16.
 */
public final class Main {

	static {
		LogUtil.init();
	}

	public static void main(final String... args)
	throws InterruptedException, IOException {

		final Config config = ConfigParser.loadDefaultConfig();
		if(config == null) {
			throw new AssertionError();
		}
		
		try {
			config.apply(CliArgParser.parseArgs(config.getAliasingConfig(), args));
		} catch(final IllegalArgumentNameException e) {
			Loggers.ERR.fatal(
				"Invalid argument: \"{}\"\nThe list of all possible args:\n{}", e.getMessage(),
				formatCliArgsList(getAllCliArgs(config))
			);
			return;
		}

		try(
			final StorageDriverBuilderSvc builderSvc = new BasicStorageDriverBuilderSvc(
				config.getStorageConfig().getDriverConfig().getPort()
			)
		) {
			builderSvc.start();
			builderSvc.await();
		} catch(final Exception e) {
			LogUtil.exception(Level.WARN, e, "Storage driver builder service failure");
		}
	}
}
