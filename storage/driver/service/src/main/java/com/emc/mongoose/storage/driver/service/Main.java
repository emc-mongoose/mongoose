package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
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

	private final static Logger LOG = LogManager.getLogger();

	public static void main(final String... args)
	throws InterruptedException {
		try(final StorageDriverBuilderSvc builderSvc = new BasicStorageDriverBuilderSvc()) {
			builderSvc.start();
			builderSvc.await();
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Storage driver builder service failure");
		}
	}
}
