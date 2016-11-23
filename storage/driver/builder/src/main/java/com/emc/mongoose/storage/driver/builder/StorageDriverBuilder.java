package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverBuilder<
	I extends Item, R extends IoResult, O extends IoTask<I, R>, T extends StorageDriver<I, R, O>
> {

	String API_ATMOS = "atmos";
	String API_S3 = "s3";
	String API_SWIFT = "swift";

	ItemConfig getItemConfig()
	throws RemoteException;

	LoadConfig getLoadConfig()
	throws RemoteException;

	StorageConfig getStorageConfig()
	throws RemoteException;

	SocketConfig getSocketConfig()
	throws RemoteException;

	StorageDriverBuilder<I, R, O, T> setJobName(final String runId)
	throws RemoteException;

	StorageDriverBuilder<I, R, O, T> setItemConfig(final ItemConfig itemConfig)
	throws RemoteException;

	StorageDriverBuilder<I, R, O, T> setLoadConfig(final LoadConfig loadConfig)
	throws RemoteException;

	StorageDriverBuilder<I, R, O, T> setStorageConfig(final StorageConfig storageConfig)
	throws RemoteException;

	StorageDriverBuilder<I, R, O, T> setSocketConfig(final SocketConfig socketConfig)
	throws RemoteException;

	T build()
	throws RemoteException, UserShootHisFootException;
}
