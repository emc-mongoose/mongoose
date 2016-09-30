package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.net.ServiceUtil;

/**
 Created on 28.09.16.
 */
public class Main {

	public static void main(String[] args)
	throws InterruptedException {
		final StorageDriverFactorySvc driverFactorySvc = new BasicStorageDriverFactorySvc();
		try {
			driverFactorySvc.start();
			driverFactorySvc.await();
			ServiceUtil.shutdown();
		} catch(final Throwable throwable) {
			throwable.printStackTrace(System.err);
		}
	}

}
