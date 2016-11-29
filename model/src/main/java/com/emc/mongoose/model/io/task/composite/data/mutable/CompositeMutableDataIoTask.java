package com.emc.mongoose.model.io.task.composite.data.mutable;

import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import static com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask.CompositeDataIoResult;
import com.emc.mongoose.model.io.task.partial.data.mutable.PartialMutableDataIoTask;
import com.emc.mongoose.model.item.MutableDataItem;

import java.util.List;
/**
 Created by andrey on 25.11.16.
 */
public interface CompositeMutableDataIoTask<
	I extends MutableDataItem, R extends CompositeDataIoResult
>
extends CompositeDataIoTask<I, R> {
	
	@Override
	List<? extends PartialMutableDataIoTask> getSubTasks();
}
