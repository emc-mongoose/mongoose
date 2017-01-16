package com.emc.mongoose.model.io.task.composite.data;

import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import static com.emc.mongoose.model.io.task.composite.CompositeIoTask.CompositeIoResult;
import com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.model.item.DataItem;

import java.util.List;
/**
 Created by andrey on 25.11.16.
 */
public interface CompositeDataIoTask<
	I extends DataItem, R extends CompositeDataIoTask.CompositeDataIoResult
>
extends CompositeIoTask<I, R> {
	
	interface CompositeDataIoResult<I extends DataItem>
	extends DataIoResult<I>, CompositeIoResult<I> {
	}

	@Override
	List<? extends PartialDataIoTask> getSubTasks();
}
