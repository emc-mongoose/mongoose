package com.emc.mongoose.core.api.data;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
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
	boolean isAppending();
	//
	long getPendingAugmentSize();
	//
	void writeAugmentFully(final WritableByteChannel chanOut)
	throws IOException;
}
