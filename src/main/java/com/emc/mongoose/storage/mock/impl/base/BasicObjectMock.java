package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.impl.data.BasicObject;
import com.emc.mongoose.core.impl.data.UniformData;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
//
/**
 * Created by olga on 22.01.15.
 */
public class BasicObjectMock
extends BasicObject
implements DataObjectMock {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicObjectMock(final String metaInfo) {
		super();
		fromString(metaInfo);
	}
	//
	public BasicObjectMock(final String id, final Long offset, final Long size) {
		super(id, offset, size);
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
				id, offset, offset + size, maskIndexStart, maskIndexEnd
			);
		}
	}
	//
	public final synchronized void append(final long offset, final long size) {
		if(size < 0) {
			throw new IllegalArgumentException(id + ": range size should not be negative");
		}
		if(this.size != offset) {
			throw new IllegalArgumentException(
				id + ": append offset " + offset + " should be equal to the current size " + this.size
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
	//
	@Override @Deprecated
	public final synchronized long writeFully(final WritableByteChannel chanOut)
	throws IOException {
		final int countRangesTotal = getRangeCount(size);
		long rangeOffset, rangeSize;
		UniformData updatedRange;
		if(hasBeenUpdated()) {
			return writeRangeFully(chanOut, 0, size);
		} else {
			long writtenCount = 0;
			for(int i = 0; i < countRangesTotal; i++) {
				rangeOffset = getRangeOffset(i);
				rangeSize = getRangeSize(i);
				if(maskRangesRead.get(i)) { // range have been modified
					updatedRange = new UniformData(
						offset + rangeOffset, rangeSize, currLayerIndex + 1,
						UniformDataSource.DEFAULT
					);
					writtenCount += updatedRange.writeFully(chanOut);
				} else if(currLayerIndex > 0) { // previous layer of updated ranges
					updatedRange = new UniformData(
						offset + rangeOffset, rangeSize, currLayerIndex,
						UniformDataSource.DEFAULT
					);
					writtenCount += updatedRange.writeFully(chanOut);
				} else { // the range was not updated
					writtenCount += writeRangeFully(chanOut, rangeOffset, rangeSize);
				}
			}
			return writtenCount;
		}
	}
}
