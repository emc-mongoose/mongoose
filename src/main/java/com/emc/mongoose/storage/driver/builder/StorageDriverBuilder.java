package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;

import com.github.akurilov.confuse.Config;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverBuilder<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
> {
	Config itemConfig();

	Config loadConfig();

	Config storageConfig();

	StorageDriverBuilder<I, O, T> classLoader(final ClassLoader clsLoader);

	StorageDriverBuilder<I, O, T> testStepId(final String runId);
	
	StorageDriverBuilder<I, O, T> dataInput(final DataInput contentSrc);

	StorageDriverBuilder<I, O, T> itemConfig(final Config itemConfig);

	StorageDriverBuilder<I, O, T> loadConfig(final Config loadConfig);

	StorageDriverBuilder<I, O, T> storageConfig(final Config storageConfig);

	T build()
	throws OmgShootMyFootException, InterruptedException;
}
