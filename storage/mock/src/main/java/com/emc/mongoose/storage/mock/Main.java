package com.emc.mongoose.storage.mock;

import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigLoader;
import com.emc.mongoose.storage.mock.impl.http.Nagaina;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static com.emc.mongoose.common.Constants.KEY_RUN_ID;
/**
 Created on 12.07.16.
 */
public class Main {

	static {
		LogUtil.init();
	}

	public static void main(final String[] args)
	throws IOException, InvocationTargetException, IllegalAccessException {
		final Config config = ConfigLoader.loadDefaultConfig();
		if(config == null) {
			throw new IllegalStateException();
		}
		config.apply(CliArgParser.parseArgs(args));
		final String newRunId = config.getRunConfig().getId();
		if(newRunId != null) {
			ThreadContext.put(KEY_RUN_ID, newRunId);
		}
		final Logger log = LogManager.getLogger();
		log.info(Markers.MSG, "Configuration loaded");
		//
		final Config.StorageConfig storageConfig = config.getStorageConfig();
		final Config.LoadConfig loadConfig = config.getLoadConfig();
		final Config.ItemConfig itemConfig = config.getItemConfig();
		try(final Nagaina nagaina = new Nagaina(storageConfig, loadConfig, itemConfig)) {
			nagaina.start();
			try {
				nagaina.await();
			} catch(final InterruptedException ignored) {
			}
		} catch(final Exception e) {
			LogUtil.exception(log, Level.ERROR, e, "Failed to run Nagaina");
		}
	}

}
