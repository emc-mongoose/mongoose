package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.config.item.ItemConfig;
import com.emc.mongoose.config.load.LoadConfig;
import com.emc.mongoose.config.storage.StorageConfig;

import java.rmi.RemoteException;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverBuilder<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
> {
	ItemConfig itemConfig();

	LoadConfig loadConfig();

	StorageConfig storageConfig();

	StorageDriverBuilder<I, O, T> classLoader(final ClassLoader clsLoader);

	StorageDriverBuilder<I, O, T> testStepId(final String runId);
	
	StorageDriverBuilder<I, O, T> dataInput(final DataInput contentSrc);

	StorageDriverBuilder<I, O, T> itemConfig(final ItemConfig itemConfig);

	StorageDriverBuilder<I, O, T> loadConfig(final LoadConfig loadConfig);

	StorageDriverBuilder<I, O, T> storageConfig(final StorageConfig storageConfig);

	T build()
	throws OmgShootMyFootException, InterruptedException;
}
