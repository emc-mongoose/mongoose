package com.emc.mongoose.core.api.item.data;
//
/**
 Created by kurila on 29.09.14.
 A data item which supports update operation.
 */
public interface UpdatableDataItem
extends DataItem {
	//
	boolean hasBeenUpdated();
	//
	boolean hasScheduledUpdates();
	//
	boolean isCurrLayerRangeUpdated(final int i);
	//
	boolean isCurrLayerRangeUpdating(final int i);
	//
	boolean isNextLayerRangeUpdating(final int i);
	//
	void scheduleRandomUpdates(final int count)
	throws IllegalArgumentException, IllegalStateException;
	//
	int getCountRangesTotal();
	//
	int getCurrLayerIndex();
	//
	long getUpdatingRangesSize();
	//
	void commitUpdatedRanges();
	//
	void resetUpdates();
	//
	long getRangeSize(final int i);
}
