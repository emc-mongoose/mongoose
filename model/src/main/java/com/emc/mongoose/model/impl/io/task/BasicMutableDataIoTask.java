package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.common.collection.ByteRange;
import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.impl.item.BasicDataItem;
import com.emc.mongoose.model.util.LoadType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 Created by andrey on 25.09.16.
 */
public final class BasicMutableDataIoTask<I extends MutableDataItem>
extends BasicDataIoTask<I>
implements MutableDataIoTask<I> {
	
	private final BitSet[] updatingRangesMask = new BitSet[] {
		new BitSet(Long.SIZE), new BitSet(Long.SIZE)
	};
	
	private BasicDataItem currRange;
	private long nextRangeOffset;
	private int currRangeIdx;
	private int currDataLayerIdx;
	
	public BasicMutableDataIoTask() {
		super();
	}
	
	public BasicMutableDataIoTask(final LoadType ioType, final I item, final String dstPath)
	throws IOException {
		super(ioType, item, dstPath);
		if(LoadType.UPDATE.equals(ioType)) {
			contentSize = getUpdatingRangesSize();
		}
	}

	@Override
	public final void reset() {
		super.reset();
		currRange = null;
		nextRangeOffset = 0;
		currRangeIdx = 0;
		currDataLayerIdx = 0;
	}
	
	@Override
	public final void scheduleRandomRangesUpdate(final int count) {
		try {
			final int countRangesTotal = MutableDataItem.getRangeCount(item.size());
			if(count < 1 || count > countRangesTotal) {
				throw new IllegalArgumentException(
					"Range count should be more than 0 and less than max " + countRangesTotal +
					" for the item size"
				);
			}
			for(int i = 0; i < count; i++) {
				scheduleRandomUpdate(countRangesTotal);
			}
		} catch(final IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private void scheduleRandomUpdate(final int countRangesTotal) {
		final int startCellPos = ThreadLocalRandom.current().nextInt(countRangesTotal);
		int nextCellPos;
		if(countRangesTotal > item.getUpdatedRangesCount() + updatingRangesMask[0].cardinality()) {
			// current layer has not updated yet ranges
			for(int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!item.isRangeUpdated(nextCellPos)) {
					if(!updatingRangesMask[0].get(nextCellPos)) {
						updatingRangesMask[0].set(nextCellPos);
						break;
					}
				}
			}
		} else {
			// update the next layer ranges
			for(int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!updatingRangesMask[0].get(nextCellPos)) {
					if(!updatingRangesMask[1].get(nextCellPos)) {
						updatingRangesMask[1].set(nextCellPos);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public final void scheduleFixedRangesUpdate(final List<ByteRange> ranges) {
		// TODO
		throw new IllegalStateException("Not implemented yet");
	}
	
	@Override
	public final long getUpdatingRangesSize() {
		return 0;
	}
	
	@Override
	public final BasicDataItem getCurrRange() {
		try {
			if(currRange == null && currRangeIdx < MutableDataItem.getRangeCount(item.size())) {
				final long currRangeSize = item.getRangeSize(currRangeIdx);
				nextRangeOffset = MutableDataItem.getRangeOffset(currRangeIdx + 1);
				if(item.isRangeUpdated(currRangeIdx)) {
					currRange = new BasicDataItem(
						item.getOffset() + nextRangeOffset, currRangeSize,
						currDataLayerIdx + 1, item.getContentSrc()
					);
				} else {
					currRange = new BasicDataItem(
						item.getOffset() + nextRangeOffset, currRangeSize,
						currDataLayerIdx, item.getContentSrc()
					);
				}
			}
		} catch(final IOException e) {
			throw new IllegalStateException(e);
		}
		return currRange;
	}

	@Override
	public final BitSet[] getUpdatingRangesMask() {
		return updatingRangesMask;
	}
	
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(updatingRangesMask[0].isEmpty() ? 0 : updatingRangesMask[0].toLongArray()[0]);
		out.writeLong(updatingRangesMask[1].isEmpty() ? 0 : updatingRangesMask[1].toLongArray()[0]);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		updatingRangesMask[0].or(BitSet.valueOf(new long[] {in.readLong()}));
		updatingRangesMask[1].or(BitSet.valueOf(new long[] {in.readLong()}));
	}
}
