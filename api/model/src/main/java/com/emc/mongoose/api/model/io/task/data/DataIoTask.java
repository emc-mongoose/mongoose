package com.emc.mongoose.api.model.io.task.data;

import com.github.akurilov.commons.collection.Range;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.DataItem;

import java.util.BitSet;
import java.util.List;

/**
 Created by kurila on 11.07.16.
 */
public interface DataIoTask<I extends DataItem>
extends IoTask<I> {
	
	@Override
	I item();
	
	void markRandomRanges(final int count);
	
	boolean hasMarkedRanges();
	
	long markedRangesSize();
	
	BitSet[] markedRangesMaskPair();
	
	List<Range> fixedRanges();

	int randomRangesCount();

	List<I> srcItemsToConcat();
	
	int currRangeIdx();
	
	void currRangeIdx(final int i);
	
	DataItem currRange();
	
	DataItem currRangeUpdate();

	long countBytesDone();

	void countBytesDone(long n);

	long respDataTimeStart();

	void startDataResponse()
	throws IllegalStateException;

	long dataLatency();
}

