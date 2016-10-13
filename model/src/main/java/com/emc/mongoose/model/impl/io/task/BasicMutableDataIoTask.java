package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.api.data.DataRangesConfig;
import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.impl.item.BasicDataItem;
import com.emc.mongoose.model.api.LoadType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.emc.mongoose.model.api.item.MutableDataItem.getRangeCount;
import static com.emc.mongoose.model.api.item.MutableDataItem.getRangeOffset;

/**
 Created by andrey on 25.09.16.
 */
public final class BasicMutableDataIoTask<I extends MutableDataItem>
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
		final LoadType ioType, final I item, final String dstPath,
		final DataRangesConfig rangesConfig
	) {
		super(ioType, item, dstPath, rangesConfig);
		if(LoadType.UPDATE.equals(ioType)) {
			final int n = rangesConfig.getRandomCount();
			if(n > 0) {
				scheduleRandomRangesUpdate(n);
			} else {
				scheduleFixedRangesUpdate(rangesConfig.getFixedByteRanges());
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
				final long nextRangeOffset = getRangeOffset(currRangeIdx + 1);
				currRange = new BasicDataItem(
					itemDataOffset + nextRangeOffset, currRangeSize, layerIdx + 1, contentSrc
				);
			} else if(updRangesMaskPair[1].get(currRangeIdx)) {
				final long currRangeSize = item.getRangeSize(currRangeIdx);
				final long nextRangeOffset = getRangeOffset(currRangeIdx + 1);
				currRange = new BasicDataItem(
					itemDataOffset + nextRangeOffset, currRangeSize, layerIdx + 2, contentSrc
				);
			} else {
				currRange = null;
			}
		}
		return currRange;
	}
	
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
