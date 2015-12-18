package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.data.ContentSource;
//
import com.emc.mongoose.core.impl.item.data.BasicMutableDataItem;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
//
/**
 * Created by olga on 22.01.15.
 */
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
	public BasicMutableDataItemMock(final String metaInfo, final ContentSource contentSrc) {
		super(metaInfo, contentSrc);
	}
	//
	public BasicMutableDataItemMock(
		final Long offset, final Long size, final ContentSource contentSrc
	) {
		super(offset, size, contentSrc);
	}
	//
	public BasicMutableDataItemMock(
		final String name, final Long offset, final Long size, final Integer layerNum,
		final ContentSource contentSrc
	) {
		super(name, offset, size, layerNum, contentSrc);
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
				currLayerIndex ++;
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
