package com.emc.mongoose.item.io.task.data;

import com.emc.mongoose.item.io.task.IoTaskBuilder;
import com.emc.mongoose.item.DataItem;
import com.github.akurilov.commons.collection.Range;

import java.util.List;

/**
 Created by kurila on 27.09.16.
 */
public interface DataIoTaskBuilder<I extends DataItem, O extends DataIoTask<I>>
extends IoTaskBuilder<I, O> {
	
	DataIoTaskBuilder<I, O> setFixedRanges(final List<Range> fixedRanges);

	DataIoTaskBuilder<I, O> setRandomRangesCount(final int count);

	DataIoTaskBuilder<I, O> setSizeThreshold(final long sizeThreshold);

	DataIoTaskBuilder<I, O> setSrcItemsCount(final int min, final int max);

	DataIoTaskBuilder<I, O> setSrcItemsForConcat(final List<I> items);
	
	List<Range> getFixedRanges();
	
	int getRandomRangesCount();
	
	long getSizeThreshold();
}
