package com.emc.mongoose.model.api.item;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.impl.data.DataCorruptionException;
import com.emc.mongoose.model.impl.data.DataSizeException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 Created by kurila on 11.07.16.
 */
public interface DataItem
extends Item, SeekableByteChannel {
	//
	ContentSource getContentSrc();
	//
	void setContentSrc(final ContentSource contentSrc);
	//
	void reset();
	//
	long getOffset();
	//
	void setOffset(final long offset);
	//
	int write(final WritableByteChannel chanDst, final long maxCount)
	throws IOException;
	//
	int readAndVerify(final ReadableByteChannel chanSrc, final ByteBuffer buff)
	throws DataSizeException, DataCorruptionException, IOException;
}
