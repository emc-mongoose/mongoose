package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.io.task.result.DataIoResult;
import com.emc.mongoose.model.item.MutableDataItem;

/**
 Created by kurila on 27.09.16.
 */
public interface MutableDataIoTaskBuilder<
	I extends MutableDataItem, R extends DataIoResult, O extends MutableDataIoTask<I, R>
>
extends DataIoTaskBuilder<I, R, O> {
	
}
