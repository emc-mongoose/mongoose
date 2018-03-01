package com.emc.mongoose.storage.driver.nio;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.cooperative.CooperativeStorageDriver;

/**
 Created by andrey on 12.05.17.
 */
public interface NioStorageDriver<I extends Item, O extends IoTask<I>>
extends CooperativeStorageDriver<I, O> {

	int MIN_TASK_BUFF_CAPACITY = 0x1000;
}
