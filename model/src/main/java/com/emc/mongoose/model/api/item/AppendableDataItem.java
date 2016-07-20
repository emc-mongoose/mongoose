package com.emc.mongoose.model.api.item;

/**
 Created on 19.07.16.
 */
interface AppendableDataItem
extends DataItem{

	void scheduleAppend(final long augmentSize)
	throws IllegalArgumentException;

	boolean isAppending();

	long getAppendSize();

	void commitAppend();
}
