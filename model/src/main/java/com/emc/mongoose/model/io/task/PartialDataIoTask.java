package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.io.task.result.PartialDataIoResult;
import com.emc.mongoose.model.item.DataItem;

/**
 Created by andrey on 23.11.16.
 */
public interface PartialDataIoTask<I extends DataItem, R extends PartialDataIoResult>
extends DataIoTask<I, R> {

	int getPartNumber();
}
