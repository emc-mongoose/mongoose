package com.emc.mongoose.api.model.io.task.data;

import com.github.akurilov.commons.collection.Range;
import com.emc.mongoose.api.model.io.task.IoTaskBuilder;
import com.emc.mongoose.api.model.item.DataItem;

import java.util.List;

/**
 Created by kurila on 27.09.16.
 */
public interface DataIoTaskBuilder<I extends DataItem, O extends DataIoTask<I>>
extends IoTaskBuilder<I, O> {
	
	DataIoTaskBuilder<I, O> setFixedRanges(final List<Range> fixedRanges);

	DataIoTaskBuilder<I, O> setRandomRangesCount(final int count);

	DataIoTaskBuilder<I, O> setSizeThreshold(final long sizeThreshold);
	
	List<Range> getFixedRanges();
	
	int getRandomRangesCount();
	
	long getSizeThreshold();
}
