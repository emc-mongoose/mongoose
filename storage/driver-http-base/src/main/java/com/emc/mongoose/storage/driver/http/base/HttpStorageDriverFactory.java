package com.emc.mongoose.storage.driver.http.base;


import com.emc.mongoose.common.pattern.EnumFactory;
import com.emc.mongoose.model.api.load.StorageDriverSvc;

import java.rmi.RemoteException;

/**
 Created on 25.09.16.
 */
public class HttpStorageDriverFactory
implements EnumFactory<String, HttpStorageDriverFactory.Api> {

	private final HttpStorageDriverConfigFactory configFactory;

	public HttpStorageDriverFactory(final HttpStorageDriverConfigFactory configFactory) {
		this.configFactory = configFactory;
	}

	@Override
	public String create(final Api type)
	throws RemoteException {
		StorageDriverSvc driver;
		switch(type) {
			case ATMOS:
				break;
			case SWIFT:
				break;
			case S3:
				break;
		}
		return null;
	}

	public enum Api {
		S3, SWIFT, ATMOS
	}
}
