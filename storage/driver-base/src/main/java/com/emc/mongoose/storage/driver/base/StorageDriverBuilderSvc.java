package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.pattern.BuilderSvc;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.storage.StorageDriver;

/**
 Created on 28.09.16.
 */
public interface StorageDriverBuilderSvc<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
	>
extends BuilderSvc<T> {
	
}
