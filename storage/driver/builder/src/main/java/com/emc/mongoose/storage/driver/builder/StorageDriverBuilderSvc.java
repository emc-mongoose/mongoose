package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.ui.config.Config.ItemConfig;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverBuilderSvc<
	I extends Item,
	R extends IoResult,
	O extends IoTask<I, R>,
	T extends StorageDriverSvc<I, R, O>
> extends StorageDriverBuilder<I, R, O, T>, Service {

	String SVC_NAME = "storage/driver/builder";

	@Override
	StorageDriverBuilderSvc<I, R, O, T> setJobName(final String jobName)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, R, O, T> setItemConfig(final ItemConfig itemConfig)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, R, O, T> setLoadConfig(final LoadConfig loadConfig)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, R, O, T> setStorageConfig(final StorageConfig storageConfig)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, R, O, T> setSocketConfig(final SocketConfig socketConfig)
	throws RemoteException;

	String buildRemotely()
	throws IOException, UserShootHisFootException;
}
