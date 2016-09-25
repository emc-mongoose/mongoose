package com.emc.mongoose.model.api.io.task;

import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.MutableDataItem;
/**
 Created by andrey on 25.09.16.
 */
public interface MutableDataIoTask<I extends MutableDataItem>
extends DataIoTask<I> {

	DataItem getCurrRange();

	long getNextRangeOffset();

	void incrementRangeIdx();
}
