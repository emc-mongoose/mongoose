package com.emc.mongoose.common.item;

import com.emc.mongoose.common.data.ContentSource;
import com.emc.mongoose.common.data.DataCorruptionException;
import com.emc.mongoose.common.data.DataSizeException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 Created by kurila on 11.07.16.
 */
public interface DataItem
extends Item, ReadableByteChannel, WritableByteChannel {
	//
	void reset();
	//
	long getSize();
	//
	void setSize(final long size);
	//
	long getOffset();
	//
	void setOffset(final long offset);
	//
	int getRelativeOffset();
	//
	void setRelativeOffset(final long relativeOffset);
	//
	void setContentSource(final ContentSource dataSrc, final int layerNum);
	//
	int write(final WritableByteChannel chanDst, final long maxCount)
	throws IOException;
	//
	int readAndVerify(final ReadableByteChannel chanSrc, final ByteBuffer buff)
	throws DataSizeException, DataCorruptionException, IOException;
}
