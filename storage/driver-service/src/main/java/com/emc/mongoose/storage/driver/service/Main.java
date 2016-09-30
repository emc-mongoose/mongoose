package com.emc.mongoose.storage.driver.service;

import java.rmi.RemoteException;

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
		} catch(final Throwable throwable) {
			throwable.printStackTrace(System.err);
		}
	}

}
