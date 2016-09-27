package com.emc.mongoose.model.api.item;

import java.util.BitSet;

/**
 Created by kurila on 29.09.14.
 Identifiable, appendable and updatable data item.
 Data item identifier is a 64-bit word.
 */
public interface MutableDataItem
extends DataItem {
	
	double LOG2 = Math.log(2);
	
	static int getRangeCount(final long size) {
		return (int) Math.ceil(Math.log(size + 1) / LOG2);
	}
	
	static long getRangeOffset(final int i) {
		return (1 << i) - 1;
	}
	
	long getRangeSize(int rangeIdx);
	
	boolean isUpdated();
	
	boolean isRangeUpdated(final int rangeIdx);
	
	int getUpdatedRangesCount();
	
	void commitUpdatedRanges(final BitSet[] updatingRangesMask);
}
