package com.emc.mongoose.api.model.io.task.composite.data;

import com.emc.mongoose.api.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.api.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.api.model.item.DataItem;

import java.util.List;

/**
 Created by andrey on 25.11.16.
 */
public interface CompositeDataIoTask<I extends DataItem>
extends CompositeIoTask<I> {
	
	@Override
	List<? extends PartialDataIoTask> getSubTasks();
}
