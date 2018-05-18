package com.emc.mongoose.item.io.task.partial.data;

import com.emc.mongoose.item.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.item.io.task.data.DataIoTask;
import com.emc.mongoose.item.io.task.partial.PartialIoTask;
import com.emc.mongoose.item.DataItem;

/**
 Created by andrey on 25.11.16.
 */
public interface PartialDataIoTask<I extends DataItem>
extends DataIoTask<I>, PartialIoTask<I> {

	@Override
	I item();

	@Override
	CompositeDataIoTask<I> parent();
}
