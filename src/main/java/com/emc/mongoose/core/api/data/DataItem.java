package com.emc.mongoose.core.api.data;
//
import com.emc.mongoose.core.api.data.src.DataSource;
//
import java.io.Closeable;
import java.io.Externalizable;
import java.io.IOException;
import java.io.OutputStream;
/**
 Created by kurila on 29.09.14.
 A most common data item descriptor having a determined size and able to be written out.
 */
public interface DataItem
extends Externalizable, Closeable {
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
	void writeTo(final OutputStream out)
	throws IOException;
	//
}
