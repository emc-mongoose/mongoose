package com.emc.mongoose.storage.mock.impl.web.data;
//
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.UniformData;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
//
import com.emc.mongoose.storage.mock.api.UpdatableDataItemMock;
//
//import org.apache.log.log4j.LogManager;
//import org.apache.log.log4j.Logger;
//
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.List;
//
/**
 * Created by olga on 22.01.15.
 */
public class BasicWSObjectMock
extends BasicWSObject
implements UpdatableDataItemMock {
	//private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSObjectMock(final String metaInfo) {
		super();
		fromString(metaInfo);
	}
	//
	public BasicWSObjectMock(final String id, final Long offset, final Long size) {
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
			if(lastCellPos < nextCellPos && maskRangesPending.get(lastCellPos)) {
				maskRangesPending.set(lastCellPos, nextCellPos);
			}
		} else {
			throw new IllegalArgumentException("Illegal append size: " + augmentSize);
		}
	}
	//
	@Override
	public final synchronized void updateRanges(final List<Long> ranges) {
		final int countRangesTotal = getRangeCount(size);
		int startCellPos,
			finishCellPos;
		for(int i = 0; i < ranges.size(); i++){
			startCellPos = getRangeCount(ranges.get(i));
			finishCellPos = getRangeCount(ranges.get(i++)) + 1;
			maskRangesPending.set(startCellPos, finishCellPos);
		}
		//return true if mask is full -> inc layer
		if(maskRangesPending.cardinality() == countRangesTotal){
			switchToNextOverlay();
		}
	}
	//
	@Override
	public final synchronized long writeFully(final WritableByteChannel chanOut)
	throws IOException {
		final int countRangesTotal = getRangeCount(size);
		long rangeOffset, rangeSize;
		UniformData updatedRange;
		if(maskRangesPending.isEmpty()) {
			return writeRange(chanOut, 0, size);
		} else {
			long writtenCount = 0;
			for(int i = 0; i < countRangesTotal; i++) {
				rangeOffset = getRangeOffset(i);
				rangeSize = getRangeSize(i);
				if(maskRangesPending.get(i)) { // range have been modified
					updatedRange = new UniformData(
						offset + rangeOffset, rangeSize, currLayerIndex + 1,
						UniformDataSource.DEFAULT
					);
					writtenCount += updatedRange.writeFully(chanOut);
				} else { // previous layer of updated ranges
					updatedRange = new UniformData(
						offset + rangeOffset, rangeSize, currLayerIndex,
						UniformDataSource.DEFAULT
					);
					writtenCount += updatedRange.writeFully(chanOut);
				}
			}
			return writtenCount;
		}
	}
	//
	@Override
	public long getContentLength() {
		return size;
	}
}
