package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.pattern.BuilderSvc;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.StorageDriver;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;

/**
 Created on 28.09.16.
 */
public interface StorageDriverBuilderSvc<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
	>
extends BuilderSvc<T> {

	StorageDriverBuilderSvc<I, O, T> setRunId(final String runId);
	StorageDriverBuilderSvc<I, O, T> setLoadConfig(final LoadConfig loadConfig);
	StorageDriverBuilderSvc<I, O, T> setSourceContainer(final String srcContainer);
	StorageDriverBuilderSvc<I, O, T> setStorageConfig(final StorageConfig storageConfig);

}
