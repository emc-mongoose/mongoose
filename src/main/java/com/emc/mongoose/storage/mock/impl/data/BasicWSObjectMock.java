package com.emc.mongoose.storage.mock.impl.data;
//
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.UniformData;
import com.emc.mongoose.core.impl.data.src.UniformDataSource;
//
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.util.List;
//
/**
 * Created by olga on 22.01.15.
 */
public class BasicWSObjectMock
extends BasicWSObject
implements WSObjectMock {
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
	//////////////////////////////////
	@Override
	public final void append(final long augmentSize)
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
	public final void updateRanges(final List<Long> ranges) {
		final int countRangesTotal = getRangeCount(size);
		int startCellPos,
			finishCellPos;
		for(int i = 0; i < ranges.size(); i ++){
			startCellPos = getRangeCount(ranges.get(i));
			finishCellPos = getRangeCount(ranges.get(i ++)) + 1;
			maskRangesPending.set(startCellPos, finishCellPos);
		}
		//return true if mask is full -> inc layer
		if(maskRangesPending.cardinality() == countRangesTotal){
			switchToNextOverlay();
		}
	}
	//
	@Override
	public final void write(final WritableByteChannel chanOut)
	throws IOException {
		final int countRangesTotal = getRangeCount(size);
		long rangeOffset, rangeSize;
		UniformData updatedRange;
		synchronized (this) { // stream position protection
			if(maskRangesPending.isEmpty()) {
				super.write(chanOut);
			} else {
				for(int i = 0; i < countRangesTotal; i++) {
					rangeOffset = getRangeOffset(i);
					rangeSize = getRangeSize(i);
					if(maskRangesPending.get(i)) { // range have been modified
						updatedRange = new UniformData(
							offset + rangeOffset, rangeSize, currLayerIndex.get() + 1,
							UniformDataSource.DEFAULT
						);
						updatedRange.write(chanOut);
					} else { // previous layer of updated ranges
						updatedRange = new UniformData(
							offset + rangeOffset, rangeSize, currLayerIndex.get(),
							UniformDataSource.DEFAULT
						);
						updatedRange.write(chanOut);
					}
				}
			}
		}
	}
}
