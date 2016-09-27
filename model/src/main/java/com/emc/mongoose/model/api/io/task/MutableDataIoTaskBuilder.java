package com.emc.mongoose.model.api.io.task;

import com.emc.mongoose.model.api.item.MutableDataItem;

/**
 Created by kurila on 27.09.16.
 */
public interface MutableDataIoTaskBuilder<I extends MutableDataItem, O extends MutableDataIoTask<I>>
extends DataIoTaskBuilder<I, O> {
	
}
