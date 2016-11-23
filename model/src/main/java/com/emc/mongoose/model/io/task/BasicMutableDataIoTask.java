package com.emc.mongoose.model.io.task;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.item.MutableDataItem;
import com.emc.mongoose.model.item.BasicDataItem;
import com.emc.mongoose.model.io.IoType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.emc.mongoose.model.item.MutableDataItem.getRangeCount;
import static com.emc.mongoose.model.item.MutableDataItem.getRangeOffset;

/**
 Created by andrey on 25.09.16.
 */
public class BasicMutableDataIoTask<I extends MutableDataItem>
extends BasicDataIoTask<I>
implements MutableDataIoTask<I> {
	
	private final BitSet[] updRangesMaskPair = new BitSet[] {
		new BitSet(Long.SIZE), new BitSet(Long.SIZE)
	};
	
	private volatile BasicDataItem currRange;
	private volatile int currRangeIdx;
	
	public BasicMutableDataIoTask() {
		super();
	}
	
	public BasicMutableDataIoTask(
		final IoType ioType, final I item, final String srcPath, final String dstPath,
		final List<ByteRange> fixedRanges, final int randomRangesCount, final long sizeThreshold
	) {
		super(ioType, item, srcPath, dstPath, fixedRanges, randomRangesCount, sizeThreshold);
		if(IoType.UPDATE.equals(ioType)) {
			if(randomRangesCount > 0) {
				scheduleRandomRangesUpdate(randomRangesCount);
			} else if(fixedRanges != null && !fixedRanges.isEmpty()){
				scheduleFixedRangesUpdate(fixedRanges);
			} else {
				throw new IllegalStateException("Range update is not configured");
			}
			contentSize = getUpdatingRangesSize();
		}
	}

	@Override
	public final void reset() {
		super.reset();
		currRange = null;
		currRangeIdx = 0;
	}
	
	@Override
	public final void scheduleRandomRangesUpdate(final int count) {
		try {
			final int countRangesTotal = getRangeCount(item.size());
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
		if(
			countRangesTotal > item.getUpdatedRangesCount() +
				updRangesMaskPair[0].cardinality()
		) {
			// current layer has not updated yet ranges
			for(int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!item.isRangeUpdated(nextCellPos)) {
					if(!updRangesMaskPair[0].get(nextCellPos)) {
						updRangesMaskPair[0].set(nextCellPos);
						break;
					}
				}
			}
		} else {
			// update the next layer ranges
			for(int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!updRangesMaskPair[0].get(nextCellPos)) {
					if(!updRangesMaskPair[1].get(nextCellPos)) {
						updRangesMaskPair[1].set(nextCellPos);
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
		long sumSize = 0;
		try {
			for(int i = 0; i < getRangeCount(item.size()); i++) {
				if(updRangesMaskPair[0].get(i) || updRangesMaskPair[1].get(i)) {
					sumSize += item.getRangeSize(i);
				}
			}
		} catch(final IOException e) {
			throw new IllegalStateException(e);
		}
		return sumSize;
	}
	
	@Override
	public final int getCurrRangeIdx() {
		return currRangeIdx;
	}
	
	@Override
	public final void setCurrRangeIdx(final int currRangeIdx) {
		currRange = null;
		this.currRangeIdx = currRangeIdx;
	}
	
	@Override
	public final BasicDataItem getCurrRange() {
		try {
			if(currRange == null && currRangeIdx < getRangeCount(item.size())) {
				final long currRangeSize = item.getRangeSize(currRangeIdx);
				final long currRangeOffset = getRangeOffset(currRangeIdx);
				final int layerIdx = item.layer();
				if(item.isRangeUpdated(currRangeIdx)) {
					currRange = new BasicDataItem(
						itemDataOffset + currRangeOffset, currRangeSize, layerIdx + 1,
						contentSrc
					);
				} else {
					currRange = new BasicDataItem(
						itemDataOffset + currRangeOffset, currRangeSize, layerIdx,
						contentSrc
					);
				}
			}
		} catch(final IOException e) {
			throw new IllegalStateException(e);
		}
		return currRange;
	}
	
	@Override
	public final BasicDataItem getCurrRangeUpdate() {
		if(currRange == null) {
			final int layerIdx = item.layer();
			if(updRangesMaskPair[0].get(currRangeIdx)) {
				final long currRangeSize = item.getRangeSize(currRangeIdx);
				final long currRangeOffset = getRangeOffset(currRangeIdx);
				currRange = new BasicDataItem(
					itemDataOffset + currRangeOffset, currRangeSize, layerIdx + 1, contentSrc
				);
			} else if(updRangesMaskPair[1].get(currRangeIdx)) {
				final long currRangeSize = item.getRangeSize(currRangeIdx);
				final long currRangeOffset = getRangeOffset(currRangeIdx);
				currRange = new BasicDataItem(
					itemDataOffset + currRangeOffset, currRangeSize, layerIdx + 2, contentSrc
				);
			} else {
				currRange = null;
			}
		}
		return currRange;
	}

	@Override
	public final BitSet[] getUpdRangesMaskPair() {
		return updRangesMaskPair;
	}
	
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(
			updRangesMaskPair[0].isEmpty() ? 0 : updRangesMaskPair[0].toLongArray()[0]
		);
		out.writeLong(
			updRangesMaskPair[1].isEmpty() ? 0 : updRangesMaskPair[1].toLongArray()[0]
		);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		updRangesMaskPair[0].or(BitSet.valueOf(new long[] {in.readLong()}));
		updRangesMaskPair[1].or(BitSet.valueOf(new long[] {in.readLong()}));
	}
}
