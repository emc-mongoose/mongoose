package com.emc.mongoose.storage.driver.service;


import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.pattern.EnumFactory;
import com.emc.mongoose.model.api.load.StorageDriverSvc;
import com.emc.mongoose.storage.driver.http.base.HttpStorageDriverConfigFactory;
import com.emc.mongoose.storage.driver.http.s3.HttpS3StorageDriverSvc;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;

/**
 Created on 25.09.16.
 */
public class HttpStorageDriverFactory
implements EnumFactory<String, HttpStorageDriverFactory.Api> {

	private static final Logger LOG = LogManager.getLogger();


	private final HttpStorageDriverConfigFactory configFactory;

	public HttpStorageDriverFactory(final HttpStorageDriverConfigFactory configFactory) {
		this.configFactory = configFactory;
	}

	@Override
	public String create(final Api type)
	throws RemoteException {
		String driverSvcName = null;
		switch(type) {
			case ATMOS:
				break;
			case SWIFT:
				break;
			case S3:
				try {
					// todo add generics
					driverSvcName = new HttpS3StorageDriverSvc(
						configFactory.getRunId(),
						configFactory.getLoadConfig(),
						configFactory.getStorageConfig(),
						configFactory.getSourceContainer(),
						configFactory.getVerifyFlag(),
						configFactory.getSocketConfig()
					).getName();
				} catch(final UserShootHisFootException e) {
					LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to create driver service instance"
					);
				}
				break;
		}
		return driverSvcName;
	}

	public enum Api {
		S3, SWIFT, ATMOS
	}
}
