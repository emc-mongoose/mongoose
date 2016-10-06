package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.storage.StorageDriver;

import java.rmi.RemoteException;

import static com.emc.mongoose.ui.config.Config.IoConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverBuilder<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
> {

	String API_S3 = "s3";
	String API_SWIFT = "swift";

	String getRunId()
	throws RemoteException;

	ItemConfig getItemConfig()
	throws RemoteException;

	LoadConfig getLoadConfig()
	throws RemoteException;

	IoConfig getIoConfig()
	throws RemoteException;

	StorageConfig getStorageConfig()
	throws RemoteException;

	SocketConfig getSocketConfig()
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setRunId(final String runId)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setItemConfig(final ItemConfig itemConfig)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setLoadConfig(final LoadConfig loadConfig)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setIoConfig(final IoConfig ioConfig)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setStorageConfig(final StorageConfig storageConfig)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setSocketConfig(final SocketConfig socketConfig)
	throws RemoteException;

	T build()
	throws RemoteException, UserShootHisFootException;
}
