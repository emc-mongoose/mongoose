package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriverSvc;
import com.emc.mongoose.ui.config.Config;

import java.rmi.RemoteException;
/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverBuilderSvc<
	I extends Item,
	O extends IoTask<I>,
	R extends IoResult,
	T extends StorageDriverSvc<I, O, R>
> extends StorageDriverBuilder<I, O, R, T>, Service {

	String SVC_NAME = "storage/driver/builder";

	@Override
	StorageDriverBuilderSvc<I, O, R, T> setJobName(final String jobName)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, O, R, T> setContentSource(final ContentSource contentSource)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, O, R, T> setItemConfig(final Config.ItemConfig itemConfig)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, O, R, T> setLoadConfig(final Config.LoadConfig loadConfig)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, O, R, T> setStorageConfig(final Config.StorageConfig storageConfig)
	throws RemoteException;

	@Override
	StorageDriverBuilderSvc<I, O, R, T> setSocketConfig(final Config.SocketConfig socketConfig)
	throws RemoteException;

	String buildRemotely()
	throws RemoteException, UserShootHisFootException;
}
