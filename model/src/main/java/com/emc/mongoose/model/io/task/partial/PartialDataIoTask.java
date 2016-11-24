package com.emc.mongoose.model.io.task.partial;

import com.emc.mongoose.model.io.task.DataIoTask;
import com.emc.mongoose.model.item.DataItem;
import static com.emc.mongoose.model.io.task.partial.PartialDataIoTask.PartialDataIoResult;

/**
 Created by andrey on 23.11.16.
 */
public interface PartialDataIoTask<I extends DataItem, R extends PartialDataIoResult>
extends DataIoTask<I, R> {
	
	interface PartialDataIoResult
	extends DataIoTask.DataIoResult {
	}
	
	int getPartNumber();
}
