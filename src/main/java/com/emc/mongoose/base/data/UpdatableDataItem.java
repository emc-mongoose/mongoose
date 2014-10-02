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
	boolean isRangeUpdatePending(final int i);
	//
	void updateRandomRange();
	//
	void updateRandomRanges(final int count);
	//
	int getPendingUpdatesCount();
	//
	int getRangeSize();
	//
	int getCountRangesTotal();
	//
	void writePendingUpdatesTo(final OutputStream out)
	throws IOException;
	//
	boolean compareWith(final InputStream in);
	//
}
