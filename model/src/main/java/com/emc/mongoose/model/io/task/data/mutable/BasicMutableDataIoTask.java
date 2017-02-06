package com.emc.mongoose.model.io.task.data.mutable;

import com.emc.mongoose.common.api.ByteRange;

import static com.emc.mongoose.model.io.IoType.UPDATE;
import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;

import com.emc.mongoose.model.io.task.data.BasicDataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.MutableDataItem;
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
public class BasicMutableDataIoTask<I extends MutableDataItem, R extends DataIoResult>
extends BasicDataIoTask<I, R>
implements MutableDataIoTask<I, R> {
	
	private final BitSet[] markedRangesMaskPair = new BitSet[] {
		new BitSet(Long.SIZE), new BitSet(Long.SIZE)
	};

	private volatile DataItem currRange;
	private volatile int currRangeIdx;
	
	public BasicMutableDataIoTask() {
		super();
	}
	
	public BasicMutableDataIoTask(
		final IoType ioType, final I item, final String srcPath, final String dstPath,
		final List<ByteRange> fixedRanges, final int randomRangesCount
	) {
		super(ioType, item, srcPath, dstPath, fixedRanges, randomRangesCount);
	}

	@Override
	public final void reset() {
		super.reset();
		markedRangesMaskPair[0].clear();
		markedRangesMaskPair[1].clear();
		if(randomRangesCount > 0) {
			markRandomRanges(randomRangesCount);
		} else if(UPDATE.equals(ioType) && (fixedRanges == null || fixedRanges.isEmpty())) {
			throw new AssertionError("Range update is not configured correctly");
		}
		contentSize = getMarkedRangesSize();
		currRange = null;
		currRangeIdx = 0;
	}
	
	@Override
	public final void markRandomRanges(final int count) {
		try {
			final int countRangesTotal = getRangeCount(item.size());
			if(count < 1 || count > countRangesTotal) {
				throw new AssertionError(
					"Range count should be more than 0 and less than max " + countRangesTotal +
					" for the item size"
				);
			}
			for(int i = 0; i < count; i ++) {
				markRandomRangesActually(countRangesTotal);
			}
		} catch(final IOException e) {
			throw new AssertionError(e);
		}
	}
	
	private void markRandomRangesActually(final int countRangesTotal) {
		final int startCellPos = ThreadLocalRandom.current().nextInt(countRangesTotal);
		int nextCellPos;
		if(countRangesTotal > item.getUpdatedRangesCount() + markedRangesMaskPair[0].cardinality()) {
			// current layer has not updated yet ranges
			for(int i = 0; i < countRangesTotal; i ++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!item.isRangeUpdated(nextCellPos)) {
					if(!markedRangesMaskPair[0].get(nextCellPos)) {
						markedRangesMaskPair[0].set(nextCellPos);
						break;
					}
				}
			}
		} else {
			// update the next layer ranges
			for(int i = 0; i < countRangesTotal; i ++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if(!markedRangesMaskPair[0].get(nextCellPos)) {
					if(!markedRangesMaskPair[1].get(nextCellPos)) {
						markedRangesMaskPair[1].set(nextCellPos);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public final long getMarkedRangesSize() {
		long sumSize = 0;
		if(fixedRanges == null || fixedRanges.isEmpty()) {
			try {
				for(int i = 0; i < getRangeCount(item.size()); i++) {
					if(markedRangesMaskPair[0].get(i) || markedRangesMaskPair[1].get(i)) {
						sumSize += item.getRangeSize(i);
					}
				}
			} catch(final IOException e) {
				throw new AssertionError(e);
			}
		} else {
			long nextBeg, nextEnd, nextSize;
			for(final ByteRange nextByteRange : fixedRanges) {
				nextBeg = nextByteRange.getBeg();
				nextEnd = nextByteRange.getEnd();
				nextSize = nextByteRange.getSize();
				if(nextSize == -1) {
					if(nextBeg == -1) {
						sumSize += nextEnd;
					} else if(nextEnd == -1) {
						try {
							sumSize += item.size() - nextBeg;
						} catch(final IOException e) {
							throw new AssertionError(e);
						}
					} else {
						sumSize += nextEnd - nextBeg + 1;
					}
				} else {
					sumSize += nextSize;
				}
			}
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
	public final DataItem getCurrRange() {
		try {
			if(currRange == null && currRangeIdx < getRangeCount(item.size())) {
				final long currRangeSize = item.getRangeSize(currRangeIdx);
				final long currRangeOffset = getRangeOffset(currRangeIdx);
				final int layerIdx = item.layer();
				currRange = item.slice(currRangeOffset, currRangeSize);
				if(item.isRangeUpdated(currRangeIdx)) {
					currRange.layer(layerIdx + 1);
				}
			}
		} catch(final IOException e) {
			throw new AssertionError(e);
		}
		return currRange;
	}
	
	@Override
	public final DataItem getCurrRangeUpdate() {
		if(currRange == null) {
			final int layerIdx = item.layer();
			if(markedRangesMaskPair[0].get(currRangeIdx)) {
				final long currRangeSize = item.getRangeSize(currRangeIdx);
				final long currRangeOffset = getRangeOffset(currRangeIdx);
				currRange = item.slice(currRangeOffset, currRangeSize);
				currRange.layer(layerIdx + 1);
			} else if(markedRangesMaskPair[1].get(currRangeIdx)) {
				final long currRangeSize = item.getRangeSize(currRangeIdx);
				final long currRangeOffset = getRangeOffset(currRangeIdx);
				currRange = item.slice(currRangeOffset, currRangeSize);
				currRange.layer(layerIdx + 2);
			} else {
				currRange = null;
			}
		}
		return currRange;
	}

	public final BitSet[] getMarkedRangesMaskPair() {
		return markedRangesMaskPair;
	}
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(
			markedRangesMaskPair[0].isEmpty() ? 0 : markedRangesMaskPair[0].toLongArray()[0]
		);
		out.writeLong(
			markedRangesMaskPair[1].isEmpty() ? 0 : markedRangesMaskPair[1].toLongArray()[0]
		);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		markedRangesMaskPair[0].or(BitSet.valueOf(new long[] {in.readLong()}));
		markedRangesMaskPair[1].or(BitSet.valueOf(new long[] {in.readLong()}));
	}
}
