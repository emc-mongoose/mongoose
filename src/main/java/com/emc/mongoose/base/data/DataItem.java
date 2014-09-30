package com.emc.mongoose.base.data;
//
import com.emc.mongoose.util.conf.RunTimeConfig;

import java.io.Closeable;
import java.io.Externalizable;
import java.io.OutputStream;
/**
 Created by kurila on 29.09.14.
 */
public interface DataItem
extends Externalizable, Closeable {
	//
	int MAX_PAGE_SIZE = (int) RunTimeConfig.getSizeBytes("data.page.size");
	//
	long getSize();
	//
	void fromString(final String v)
	throws IllegalArgumentException, NullPointerException;
	//
	void writeTo(final OutputStream out);
	//
}
