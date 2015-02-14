package com.emc.mongoose.base.data;
//
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 Created by kurila on 29.09.14.
 A data item which supports update operation.
 */
public interface UpdatableDataItem
extends DataItem {
	//
	int getLayerNum();
	//
	boolean isRangeUpdatePending(final int i);
	//
	void updateRandomRange();
	//
	void updateRandomRanges(final int count);
	//
	int getCountRangesTotal();
	//
	long getPendingRangesSize();
	//
	void writePendingUpdatesTo(final OutputStream out)
	throws IOException;
	//
	InputStream getPendingUpdatesContent()
	throws IOException;
	//
	boolean isContentEqualTo(final InputStream in)
	throws IOException;
	//
	public long getRangeSize(final int i);
}
