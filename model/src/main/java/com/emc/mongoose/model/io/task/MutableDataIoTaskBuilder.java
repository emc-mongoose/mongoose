package com.emc.mongoose.model.io.task;

import static com.emc.mongoose.model.io.task.DataIoTask.DataIoResult;
import com.emc.mongoose.model.item.MutableDataItem;

/**
 Created by kurila on 27.09.16.
 */
public interface MutableDataIoTaskBuilder<
	I extends MutableDataItem, O extends MutableDataIoTask<I, R>, R extends DataIoResult
>
extends DataIoTaskBuilder<I, O, R> {
	
}
