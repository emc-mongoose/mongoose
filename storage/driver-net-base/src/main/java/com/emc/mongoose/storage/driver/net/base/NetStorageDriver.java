package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.storage.StorageDriver;

/**
 Created by kurila on 30.09.16.
 */
public interface NetStorageDriver<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O> {
	
}
