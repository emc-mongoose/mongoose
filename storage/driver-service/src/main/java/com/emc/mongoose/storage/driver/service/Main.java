package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 Created on 28.09.16.
 */
public class Main {

	static {
		LogUtil.init();
	}

	public static void main(String[] args)
	throws InterruptedException {
		final Logger log = LogManager.getLogger();
		final StorageDriverFactorySvc driverFactorySvc = new BasicStorageDriverFactorySvc();
		try {
			driverFactorySvc.start();
			log.info(Markers.MSG, "Load driver service started");
			driverFactorySvc.await();
			ServiceUtil.shutdown();
		} catch(final Throwable throwable) {
			throwable.printStackTrace(System.err);
		}
	}

}
