package com.emc.mongoose.base.data;
//
import java.io.Closeable;
import java.io.Externalizable;
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
	long getOffset();
	//
	void writeTo(final OutputStream out);
	//
}
