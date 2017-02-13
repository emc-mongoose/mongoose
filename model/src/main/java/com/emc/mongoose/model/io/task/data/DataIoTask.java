package com.emc.mongoose.model.io.task.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.DataItem;

import java.util.BitSet;
import java.util.List;

/**
 Created by kurila on 11.07.16.
 */
public interface DataIoTask<I extends DataItem>
extends IoTask<I> {
	
	@Override
	I getItem();
	
	void markRandomRanges(final int count);
	
	boolean hasMarkedRanges();
	
	long getMarkedRangesSize();
	
	BitSet[] getMarkedRangesMaskPair();
	
	List<ByteRange> getFixedRanges();
	
	int getCurrRangeIdx();
	
	void setCurrRangeIdx(final int i);
	
	DataItem getCurrRange();
	
	DataItem getCurrRangeUpdate();

	long getCountBytesDone();

	void setCountBytesDone(long n);

	long getRespDataTimeStart();

	void startDataResponse();
}

