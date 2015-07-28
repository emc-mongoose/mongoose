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
	void updateRandomRange();
	//
	void updateRandomRanges(final int count);
	//
	int getCountRangesTotal();
	//
	long getPendingRangesSize();
	//
	void writeUpdatedRangesFully(final WritableByteChannel chanOut)
	throws IOException;
	//
	public long getRangeSize(final int i);
}
