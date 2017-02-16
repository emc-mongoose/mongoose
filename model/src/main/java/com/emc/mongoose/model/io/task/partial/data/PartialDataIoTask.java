package com.emc.mongoose.model.io.task.partial.data;

import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.item.DataItem;

/**
 Created by andrey on 25.11.16.
 */
public interface PartialDataIoTask<I extends DataItem>
extends DataIoTask<I>, PartialIoTask<I> {

	@Override
	I getItem();

	@Override
	CompositeDataIoTask<I> getParent();
}
