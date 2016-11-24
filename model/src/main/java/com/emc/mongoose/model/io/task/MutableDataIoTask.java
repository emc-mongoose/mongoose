package com.emc.mongoose.model.io.task;

import com.emc.mongoose.common.api.ByteRange;
import static com.emc.mongoose.model.io.task.DataIoTask.DataIoResult;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.MutableDataItem;

import java.util.BitSet;
import java.util.List;

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
	
	void scheduleRandomRangesUpdate(final int count);
	
	void scheduleFixedRangesUpdate(final List<ByteRange> ranges);
	
	long getUpdatingRangesSize();
	
	DataItem getCurrRangeUpdate();
	
	BitSet[] getUpdRangesMaskPair();
}
