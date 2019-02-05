package com.emc.mongoose.storage.driver.coop.nio;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.storage.driver.StorageDriver;

/**
 Created by andrey on 12.05.17.
 */
public interface NioStorageDriver<I extends Item, O extends Operation<I>>
extends StorageDriver<I, O> {

	int MIN_TASK_BUFF_CAPACITY = 0x1000;
}
