package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.data.DataRangesConfig;
import com.emc.mongoose.model.item.DataItem;

/**
 Created by kurila on 27.09.16.
 */
public interface DataIoTaskBuilder<I extends DataItem, O extends DataIoTask<I>>
extends IoTaskBuilder<I, O> {
	
	DataIoTaskBuilder<I, O> setRangesConfig(final DataRangesConfig rangesConfig);
}
