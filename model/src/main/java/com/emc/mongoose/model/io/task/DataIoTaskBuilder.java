package com.emc.mongoose.model.io.task;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.io.task.result.DataIoResult;
import com.emc.mongoose.model.item.DataItem;

import java.util.List;
/**
 Created by kurila on 27.09.16.
 */
public interface DataIoTaskBuilder<I extends DataItem, R extends DataIoResult, O extends DataIoTask<I, R>>
extends IoTaskBuilder<I, R, O> {
	
	DataIoTaskBuilder<I, R, O> setFixedRanges(final List<ByteRange> fixedRanges);

	DataIoTaskBuilder<I, R, O> setRandomRangesCount(final int count);

	DataIoTaskBuilder<I, R, O> setSizeThreshold(final long sizeThreshold);
}
