package com.emc.mongoose.web.storagemock;
//
import com.emc.mongoose.base.data.impl.UniformData;
import com.emc.mongoose.base.data.impl.UniformDataSource;
import com.emc.mongoose.object.data.impl.BasicObject;
//
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
//
/**
 * Created by olga on 22.01.15.
 */
public class DataServlet
extends BasicObject{
	//////////////////////////////////
	public DataServlet() {
		super();
	}
	//
	public DataServlet(final String id, final Long offset, final long size) {
		super(id, offset, size);
	}
	//////////////////////////////////
	@Override
	public final void append(final long augmentSize)
			throws IllegalArgumentException {
		if(augmentSize > 0) {
			pendingAugmentSize = augmentSize;
			final int
					lastCellPos = getRangeCount(size)-1,
					nextCellPos = getRangeCount(size + augmentSize);
			if(lastCellPos < nextCellPos && maskRangesPending.get(lastCellPos)) {
				maskRangesPending.set(lastCellPos, nextCellPos);
			}
		} else {
			throw new IllegalArgumentException(
					String.format(FMT_MSG_ILLEGAL_APPEND_SIZE, augmentSize)
			);
		}
	}
	//
	public final void updateRanges(final List<Long> ranges){
		final int countRangesTotal = getRangeCount(size);
		int startCellPos,
				finishCellPos;
		for (int i = 0; i < ranges.size(); i++){
			startCellPos = getRangeCount(ranges.get(i));
			finishCellPos = getRangeCount(ranges.get(i++))+1;
			maskRangesPending.set(startCellPos, finishCellPos);
		}
		//return true if mask is full -> inc layer
		if (maskRangesPending.cardinality() == countRangesTotal){
			switchToNextLayer();
		}
	}
	//
	@Override
	public void writeTo(final OutputStream out)
			throws IOException {
		final int countRangesTotal = getRangeCount(size);
		long rangeOffset, rangeSize;
		UniformData updatedRange;
		synchronized (this) {
			if (maskRangesPending.isEmpty()) {
				super.writeTo(out);
			} else {
				for (int i = 0; i < countRangesTotal; i++) {
					rangeOffset = getRangeOffset(i);
					rangeSize = getRangeSize(i);
					if (maskRangesPending.get(i)) { // range have been modified
						updatedRange = new UniformData(
								offset + rangeOffset, rangeSize, layerNum + 1, UniformDataSource.DEFAULT
						);
						updatedRange.writeTo(out);
					} else { // previous layer of updated ranges
						updatedRange = new UniformData(
								offset + rangeOffset, rangeSize, layerNum, UniformDataSource.DEFAULT
						);
						updatedRange.writeTo(out);
					}
				}
			}
		}

	}
}
