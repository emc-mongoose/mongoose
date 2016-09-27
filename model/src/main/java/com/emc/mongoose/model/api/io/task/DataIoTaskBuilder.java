package com.emc.mongoose.model.api.io.task;

import com.emc.mongoose.model.api.data.DataRangesConfig;
import com.emc.mongoose.model.api.item.DataItem;

/**
 Created by kurila on 27.09.16.
 */
public interface DataIoTaskBuilder<I extends DataItem, O extends DataIoTask<I>>
extends IoTaskBuilder<I, O> {
	
	DataIoTaskBuilder<I, O> setRangesConfig(final DataRangesConfig rangesConfig);
}
