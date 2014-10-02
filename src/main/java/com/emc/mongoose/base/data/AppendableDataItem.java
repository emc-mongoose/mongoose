package com.emc.mongoose.base.data;
import java.io.IOException;
import java.io.OutputStream;
/**
 Created by kurila on 29.09.14.
 A data item which supports append operation.
 */
public interface AppendableDataItem
extends DataItem {
	//
	void append(final long augmentSize);
	//
	long getPendingAugmentSize();
	//
	void writeAugmentTo(final OutputStream out)
	throws IOException;
	//
}
