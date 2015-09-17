package com.emc.mongoose.core.api.data;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
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
	void scheduleRandomUpdate()
	throws IllegalStateException;
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
	@Deprecated
	long writeUpdatedRangesFully(final WritableByteChannel chanOut)
	throws IOException;
	//
	void commitUpdatedRanges();
	//
	void resetUpdates();
	//
	long getRangeSize(final int i);
}
