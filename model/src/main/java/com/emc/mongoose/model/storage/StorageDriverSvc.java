package com.emc.mongoose.model.storage;

import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverSvc<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O>, Service {
}
