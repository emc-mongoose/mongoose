package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.impl.item.BasicMutableDataItem;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BasicMutableDataItemMock
extends BasicMutableDataItem
implements MutableDataItemMock {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicMutableDataItemMock() {
		super();
	}
	//
	public BasicMutableDataItemMock(final String value, final ContentSource contentSrc) {
		super(value, contentSrc);
	}
	//
	public BasicMutableDataItemMock(
		final long offset, final long size, final ContentSource contentSrc
	) {
		super(offset, size, contentSrc);
	}
	//
	public BasicMutableDataItemMock(
		final String name, final long offset, final long size, final ContentSource contentSrc
	) {
		super(name, offset, size, contentSrc);
	}
	//
	public BasicMutableDataItemMock(
		final String name, final long offset, final long size, final int layerNum,
		final ContentSource contentSrc
	) {
		super(SLASH, name, offset, size, layerNum, contentSrc);
	}
	//
	public final synchronized void update(final long offset, final long size)
	throws IllegalArgumentException, IllegalStateException {
		if(size < 0) {
			throw new IllegalArgumentException("Range size should not be negative");
		}
		final int
			countRangesTotal = getRangeCount(this.size),
			maskIndexStart = getRangeCount(offset),
			maskIndexEnd = getRangeCount(offset + size);
		for(int i = maskIndexStart; i < maskIndexEnd; i ++) {
			if(countRangesTotal > 0 && countRangesTotal == maskRangesRead.cardinality()) {
				// mask is full, switch to the next layer
				layerNum ++;
				maskRangesRead.clear();
			}
			if(maskRangesRead.get(i)) {
				throw new IllegalStateException(
					"Range " + i + " is already updated, but mask is: " + maskRangesRead.toString()
				);
			} else {
				maskRangesRead.set(i);
			}
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "{}: byte range {}-{} updated, mask range {}-{} is set",
				name, offset, offset + size, maskIndexStart, maskIndexEnd
			);
		}
	}
	//
	public final synchronized void append(final long offset, final long size) {
		if(size < 0) {
			throw new IllegalArgumentException(name + ": range size should not be negative");
		}
		if(this.size != offset) {
			throw new IllegalArgumentException(
				name + ": append offset " + offset + " should be equal to the current size " + this.size
			);
		}
		final int
			lastCellPos = this.size > 0 ? getRangeCount(this.size) - 1 : 0,
			nextCellPos = getRangeCount(this.size + size);
		if(lastCellPos < nextCellPos && maskRangesRead.get(lastCellPos)) {
			maskRangesRead.set(lastCellPos, nextCellPos);
		}
		this.size += size;
	}
}
