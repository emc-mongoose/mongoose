package com.emc.mongoose.base.data;
//
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 Created by kurila on 29.09.14.
 A data item which supports append operation.
 */
public interface AppendableDataItem
extends DataItem {
	//
	void append(final long augmentSize)
	throws IllegalArgumentException;
	//
	long getPendingAugmentSize();
	//
	void writeAugmentTo(final OutputStream out)
	throws IOException;
	//
	InputStream getAugmentContent()
	throws IOException;
}
