package com.emc.mongoose.storage.driver.service;


import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.pattern.EnumFactory;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.storage.driver.http.base.HttpStorageDriverConfigFactory;
import com.emc.mongoose.storage.driver.http.s3.HttpS3StorageDriverSvc;
import com.emc.mongoose.ui.config.Config.StorageConfig.HttpConfig.Api;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;

/**
 Created on 25.09.16.
 */
public class HttpStorageDriverFactory<I extends Item, O extends IoTask<I>>
implements EnumFactory<String, Api> {

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
					driverSvcName = new HttpS3StorageDriverSvc<I, O>(
						configFactory.getRunId(),
						configFactory.getLoadConfig(),
						configFactory.getSourceContainer(),
						configFactory.getStorageConfig(),
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

}
