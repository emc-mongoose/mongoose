package com.emc.mongoose.api.model.io.task.partial.data;

import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.io.task.composite.data.CompositeDataIoTask;

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
