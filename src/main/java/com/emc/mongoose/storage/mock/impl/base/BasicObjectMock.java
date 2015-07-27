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
	@Override
	public final synchronized void append(final long augmentSize)
	throws IllegalArgumentException {
		if(augmentSize > 0) {
			pendingAugmentSize = augmentSize;
			final int
				lastCellPos = getRangeCount(size) - 1,
				nextCellPos = getRangeCount(size + augmentSize);
			if(lastCellPos < nextCellPos && maskRangesHistory.get(lastCellPos)) {
				maskRangesHistory.set(lastCellPos, nextCellPos);
			}
		} else {
			throw new IllegalArgumentException(id + ": illegal append size: " + augmentSize);
		}
	}
	//
	public final synchronized void update(final long offset, final long size) {
		if(size < 0) {
			throw new IllegalArgumentException("Range size should not be negative");
		}
		final int
			maskIndexStart = getRangeCount(offset),
			maskIndexEnd = getRangeCount(offset + size);
		if(maskIndexStart == maskIndexEnd) {
			maskRangesHistory.set(maskIndexStart);
		} else {
			maskRangesHistory.set(maskIndexStart, maskIndexEnd);
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
			maskIndexStart = getRangeCount(offset),
			maskIndexEnd = getRangeCount(offset + size);
		this.size += size;
		if(maskRangesHistory.get(maskIndexStart) && maskIndexEnd > maskIndexStart) {
			maskRangesHistory.set(maskIndexStart, maskIndexEnd);
		}
	}
	//
	@Override
	public final synchronized long writeFully(final WritableByteChannel chanOut)
	throws IOException {
		final int countRangesTotal = getRangeCount(size);
		long rangeOffset, rangeSize;
		UniformData updatedRange;
		if (maskRangesHistory.isEmpty()) {
			return writeRange(chanOut, 0, size);
		} else {
			long writtenCount = 0;
			for(int i = 0; i < countRangesTotal; i++) {
				rangeOffset = getRangeOffset(i);
				rangeSize = getRangeSize(i);
				if(maskRangesHistory.get(i)) { // range have been modified
					updatedRange = new UniformData(
						offset + rangeOffset, rangeSize, currLayerIndex + 1,
						UniformDataSource.DEFAULT
					);
					writtenCount += updatedRange.writeFully(chanOut);
				} else if(currLayerIndex > 0 ){ // previous layer of updated ranges
					updatedRange = new UniformData(
						offset + rangeOffset, rangeSize, currLayerIndex,
						UniformDataSource.DEFAULT
					);
					writtenCount += updatedRange.writeFully(chanOut);
				} else { // the range was not updated
					writtenCount += writeRange(chanOut, rangeOffset, rangeSize);
				}
			}
			return writtenCount;
		}
	}
}
