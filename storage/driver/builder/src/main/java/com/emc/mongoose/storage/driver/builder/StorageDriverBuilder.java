package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.MetricsConfig;

import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;

import java.rmi.RemoteException;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverBuilder<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
> {

	ItemConfig getItemConfig()
	throws RemoteException;

	LoadConfig getLoadConfig()
	throws RemoteException;

	MetricsConfig getMetricsConfig()
	throws RemoteException;

	StorageConfig getStorageConfig()
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setTestStepName(final String runId)
	throws RemoteException;
	
	StorageDriverBuilder<I, O, T> setContentSource(final ContentSource contentSrc)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setItemConfig(final ItemConfig itemConfig)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setLoadConfig(final LoadConfig loadConfig)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setMetricsConfig(final MetricsConfig metricsConfig)
	throws RemoteException;

	StorageDriverBuilder<I, O, T> setStorageConfig(final StorageConfig storageConfig)
	throws RemoteException;

	T build()
	throws RemoteException, UserShootHisFootException;
}
