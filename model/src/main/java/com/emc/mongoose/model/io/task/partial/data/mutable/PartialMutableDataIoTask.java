package com.emc.mongoose.model.io.task.partial.data.mutable;

import static com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask.PartialDataIoResult;
import com.emc.mongoose.model.io.task.composite.data.mutable.CompositeMutableDataIoTask;
import com.emc.mongoose.model.io.task.data.mutable.MutableDataIoTask;
import com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.model.item.MutableDataItem;

/**
 Created by andrey on 25.11.16.
 */
public interface PartialMutableDataIoTask<I extends MutableDataItem, R extends PartialDataIoResult>
extends PartialDataIoTask<I, R>, MutableDataIoTask<I, R> {
	
	@Override
	I getItem();
	
	@Override
	CompositeMutableDataIoTask<I, ? extends DataIoResult> getParent();
}
