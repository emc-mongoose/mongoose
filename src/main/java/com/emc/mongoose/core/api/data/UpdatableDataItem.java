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
	boolean hasAnyUpdatedRanges();
	//
	boolean isCurrLayerRangeUpdating(final int i);
	//
	boolean isNextLayerRangeUpdating(final int i);
	//
	void updateRandomRange()
	throws IllegalStateException;
	//
	void updateRandomRanges(final int count)
	throws IllegalArgumentException, IllegalStateException;
	//
	int getCountRangesTotal();
	//
	int getCurrLayerIndex();
	//
	long getPendingRangesSize();
	//
	long writeUpdatedRangesFully(final WritableByteChannel chanOut)
	throws IOException;
	//
	long getRangeSize(final int i);
}
