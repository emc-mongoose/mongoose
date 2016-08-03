package com.emc.mongoose.model.api.item;

/**
 Created on 19.07.16.
 */
interface UpdatableDataItem
extends DataItem {

	boolean hasBeenUpdated();

	boolean hasScheduledUpdates();

	boolean isCurrLayerRangeUpdated(final int rangeNum);

	boolean isCurrLayerRangeUpdating(final int rangeNum);

	boolean isNextLayerRangeUpdating(final int rangeNum);

	void scheduleRandomUpdates(final int count)
	throws IllegalArgumentException, IllegalStateException;

	int getCountRangesTotal();

	int getCurrLayerIndex();

	long getUpdatingRangesSize();

	void commitUpdatedRanges();

	void resetUpdates();

	long getRangeSize(final int i);

}
