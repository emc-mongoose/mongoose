package com.emc.mongoose.base.data.impl;
//
import com.emc.mongoose.base.data.TimedOutputDataItem;

import java.io.IOException;
import java.io.OutputStream;
/**
 Created by kurila on 01.12.14.
 */
public class DataItemBase
extends DataRanges
implements TimedOutputDataItem {
	////////////////////////////////////////////////////////////////////////////////////////////////
	public DataItemBase() {
		super(); // ranges remain uninitialized
	}
	//
	public DataItemBase(final String metaInfo) {
		fromString(metaInfo); // invokes ranges initialization
	}
	//
	public DataItemBase(final long size) {
		super(size);
	}
	//
	public DataItemBase(final long size, final UniformDataSource dataSrc) {
		super(size, dataSrc);
	}
	//
	public DataItemBase(final long offset, final long size) {
		super(offset, size);
	}
	//
	public DataItemBase(final long offset, final long size, final UniformDataSource dataSrc) {
		super(offset, size, dataSrc);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private volatile long tsSent = 0;
	//
	@Override
	public long getSentTimeStamp() {
		return tsSent;
	}
	//
	@Override
	public final void writeTo(final OutputStream out)
	throws IOException {
		try {
			super.writeTo(out);
		} finally {
			tsSent = System.nanoTime();
		}
	}
	//
	@Override
	public final void writePendingUpdatesTo(final OutputStream out)
	throws IOException {
		try {
			super.writePendingUpdatesTo(out);
		} finally {
			tsSent = System.nanoTime();
		}
	}
	//
	@Override
	public final void writeAugmentTo(final OutputStream out)
	throws IOException {
		try {
			super.writeAugmentTo(out);
		} finally {
			tsSent = System.nanoTime();
		}
	}
}
