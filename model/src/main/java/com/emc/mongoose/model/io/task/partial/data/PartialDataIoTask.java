package com.emc.mongoose.model.io.task.partial.data;

import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.item.DataItem;

/**
 Created by andrey on 25.11.16.
 */
public interface PartialDataIoTask<
	I extends DataItem, R extends PartialDataIoTask.PartialDataIoResult
>
extends DataIoTask<I, R>, PartialIoTask<I, R> {

	interface PartialDataIoResult<I extends DataItem>
	extends DataIoResult<I>, PartialIoResult<I> {
	}

	@Override
	CompositeDataIoTask<I, ? extends DataIoResult> getParent();
}
