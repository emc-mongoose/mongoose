package com.emc.mongoose.storage.driver.cooperative;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;

public interface CooperativeStorageDriver<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O> {
}
