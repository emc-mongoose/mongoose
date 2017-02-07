package com.emc.mongoose.model.io.task.data.mutable;

import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;

import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.MutableDataItem;

import java.util.BitSet;

/**
 Created by andrey on 25.09.16.
 */
public interface MutableDataIoTask<I extends MutableDataItem, R extends DataIoResult>
extends DataIoTask<I, R> {

	@Override
	I getItem();
	
	int getCurrRangeIdx();
	
	void setCurrRangeIdx(final int i);
	
	DataItem getCurrRange();
	
	void markRandomRanges(final int count);

	boolean hasMarkedRanges();
	
	long getMarkedRangesSize();
	
	DataItem getCurrRangeUpdate();
	
	BitSet[] getMarkedRangesMaskPair();
}
