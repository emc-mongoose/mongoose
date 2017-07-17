package com.emc.mongoose.storage.driver.nio.base;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;

/**
 Created by andrey on 12.05.17.
 */
public interface NioStorageDriver<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O> {
	int MIN_TASK_BUFF_CAPACITY = 0x1000;
}
