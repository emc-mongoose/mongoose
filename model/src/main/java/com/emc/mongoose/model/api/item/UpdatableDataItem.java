package com.emc.mongoose.model.api.item;

/**
 Created on 19.07.16.
 */
interface UpdatableDataItem
extends DataItem {

	boolean hasBeenUpdated();

	boolean hasScheduledUpdates();

	boolean isCurrLayerRangeUpdated(final int layerNum);

	boolean isCurrLayerRangeUpdating(final int layerNum);

	boolean isNextLayerRangeUpdating(final int layerNum);

	void scheduleRandomUpdates(final int count)
	throws IllegalArgumentException, IllegalStateException;

	int getCountRangesTotal();

	int getCurrLayerIndex();

	long getUpdatingRangesSize();

	void commitUpdatedRanges();

	void resetUpdates();

	long getRangeSize(final int i);

}
