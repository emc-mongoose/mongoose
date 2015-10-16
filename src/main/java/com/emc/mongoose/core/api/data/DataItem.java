package com.emc.mongoose.core.api.data;
//
import com.emc.mongoose.core.api.data.content.ContentSource;
//
import java.io.Externalizable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
/**
 Created by kurila on 29.09.14.
 A most common data item descriptor having a determined size and able to be written out.
 */
public interface DataItem
extends ReadableByteChannel, Externalizable {
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
