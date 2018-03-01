package com.emc.mongoose.storage.driver.preemptive;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;

public interface PreemptiveStorageDriver<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O> {
}
