package com.emc.mongoose.core.api.data;
//
import com.emc.mongoose.core.api.data.util.DataSource;
//
import java.io.Externalizable;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
/**
 Created by kurila on 29.09.14.
 A most common data item descriptor having a determined size and able to be written out.
 */
public interface DataItem
extends ReadableByteChannel, Externalizable {
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
	void setDataSource(final DataSource dataSrc, final int layerNum);
	//
	long write(final WritableByteChannel chanDst)
	throws IOException;
	//
	boolean equals(final ReadableByteChannel chanSrc)
	throws IOException;
}
