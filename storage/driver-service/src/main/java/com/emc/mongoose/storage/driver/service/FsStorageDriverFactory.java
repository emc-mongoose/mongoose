package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.pattern.SingleFactory;
import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.storage.driver.fs.BasicFileStorageDriverSvc;
import com.emc.mongoose.storage.driver.fs.FsStorageDriverConfigFactory;

import java.rmi.RemoteException;

/**
 Created on 30.09.16.
 */
public class FsStorageDriverFactory<I extends MutableDataItem, O extends MutableDataIoTask<I>>
implements SingleFactory<String> {

	private final FsStorageDriverConfigFactory configFactory;

	public FsStorageDriverFactory(final FsStorageDriverConfigFactory configFactory) {
		this.configFactory = configFactory;
	}

	@Override
	public String create()
	throws RemoteException {
		return new BasicFileStorageDriverSvc<I, O>(
			configFactory.getRunId(),
			configFactory.getLoadConfig(),
			configFactory.getSourceContainer(),
			configFactory.getStorageConfig(),
			configFactory.getVerifyFlag(),
			configFactory.getIoBuffSize()
		).getName();
	}
}
