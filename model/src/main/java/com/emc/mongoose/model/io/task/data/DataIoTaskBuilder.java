package com.emc.mongoose.model.io.task.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import com.emc.mongoose.model.item.DataItem;

import java.util.List;

/**
 Created by kurila on 27.09.16.
 */
public interface DataIoTaskBuilder<
	I extends DataItem, O extends DataIoTask<I, R>, R extends DataIoResult<I>
> extends IoTaskBuilder<I, O, R> {
	
	DataIoTaskBuilder<I, O, R> setFixedRanges(final List<ByteRange> fixedRanges);

	DataIoTaskBuilder<I, O, R> setRandomRangesCount(final int count);

	DataIoTaskBuilder<I, O, R> setSizeThreshold(final long sizeThreshold);
}
