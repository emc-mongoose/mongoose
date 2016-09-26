package com.emc.mongoose.model.api.io.task;

import com.emc.mongoose.common.collection.ByteRange;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.MutableDataItem;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;

/**
 Created by andrey on 25.09.16.
 */
public interface MutableDataIoTask<I extends MutableDataItem>
extends DataIoTask<I> {

	void scheduleRandomRangesUpdate(final int count);
	
	void scheduleFixedRangesUpdate(final List<ByteRange> ranges);
	
	long getUpdatingRangesSize();
	
	DataItem getCurrRange();
	
	BitSet[] getUpdatingRangesMask();
	
	
}
