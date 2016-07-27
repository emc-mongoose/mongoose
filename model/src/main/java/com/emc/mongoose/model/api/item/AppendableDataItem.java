package com.emc.mongoose.model.api.item;

/**
 Created by kurila on 29.09.14.
 A data item which supports append operation.
 */
interface AppendableDataItem
extends DataItem{

	void scheduleAppend(final long augmentSize)
	throws IllegalArgumentException;

	boolean isAppending();

	long getAppendSize();

	void commitAppend();
}
