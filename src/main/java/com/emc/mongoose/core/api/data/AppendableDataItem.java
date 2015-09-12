package com.emc.mongoose.core.api.data;
//
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 29.09.14.
 A data item which supports append operation.
 */
public interface AppendableDataItem
extends DataItem {
	//
	void scheduleAppend(final long augmentSize)
	throws IllegalArgumentException;
	//
	boolean isAppending();
	//
	long getAppendSize();
	//
	void commitAppend();
}
