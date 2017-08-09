package com.emc.mongoose.api.model.storage;

import com.emc.mongoose.api.model.svc.Service;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverSvc<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O>, Service {
}
