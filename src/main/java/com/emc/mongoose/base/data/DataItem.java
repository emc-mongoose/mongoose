package com.emc.mongoose.base.data;
//
import com.emc.mongoose.util.conf.RunTimeConfig;

import javax.xml.crypto.Data;
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
	int MAX_PAGE_SIZE = (int) RunTimeConfig.getSizeBytes("data.page.size");
	//
	long getSize();
	//
	void writeTo(final OutputStream out);
	//
}
