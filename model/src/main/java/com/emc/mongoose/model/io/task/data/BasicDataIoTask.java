package com.emc.mongoose.model.io.task.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.task.BasicIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.io.IoType;
import static com.emc.mongoose.model.io.IoType.UPDATE;
import static com.emc.mongoose.model.item.DataItem.getRangeCount;
import static com.emc.mongoose.model.item.DataItem.getRangeOffset;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import static java.lang.System.nanoTime;

/**
 Created by andrey on 25.09.16.
 */
public class BasicDataIoTask<T extends DataItem, R extends DataIoTask.DataIoResult>
extends BasicIoTask<T, R>
implements DataIoTask<T, R> {
	
	protected long contentSize;
	protected final BitSet[] markedRangesMaskPair = new BitSet[] {
		new BitSet(Long.SIZE), new BitSet(Long.SIZE)
	};
	private int randomRangesCount;
	private List<ByteRange> fixedRanges;
	
	protected transient volatile ContentSource contentSrc;
	protected transient volatile long countBytesDone;
	protected transient volatile long respDataTimeStart;
	private volatile DataItem currRange;
	private volatile int currRangeIdx;
	
	public BasicDataIoTask() {
		super();
	}
	
	public BasicDataIoTask(
		final int originCode, final IoType ioType, final T item, final String srcPath,
		final String dstPath, final List<ByteRange> fixedRanges, final int randomRangesCount
	) {
		super(originCode, ioType, item, srcPath, dstPath);
		this.fixedRanges = fixedRanges;
		this.randomRangesCount = randomRangesCount;
		item.reset();
		contentSrc = item.getContentSrc();
	}
	
	public static class BasicDataIoResult<T extends DataItem>
	extends BasicIoResult<T>
	implements DataIoResult<T> {
		
		private long dataLatency;
		private long transferredByteCount;

		public BasicDataIoResult() {
			super();
		}
		
		public BasicDataIoResult(
			final String storageDriverAddr, final String storageNodeAddr, final T item,
			final int ioTypeCode, final int statusCode, final long reqTimeStart,
			final long duration, final long latency, final long dataLatency,
			final long transferredByteCount
		) {
			super(
				storageDriverAddr, storageNodeAddr, item, ioTypeCode, statusCode, reqTimeStart,
				duration, latency
			);
			this.dataLatency = dataLatency > latency && duration > latency ? dataLatency : -1;
			this.transferredByteCount = transferredByteCount;
		}
		
		@Override
		public final long getDataLatency() {
			return dataLatency;
		}
		
		@Override
		public final long getCountBytesDone() {
			return transferredByteCount;
		}
		
		@Override
		public void writeExternal(final ObjectOutput out)
		throws IOException {
			super.writeExternal(out);
			out.writeLong(dataLatency);
			out.writeLong(transferredByteCount);
		}
		
		@Override
		public void readExternal(final ObjectInput in)
		throws IOException, ClassNotFoundException {
			super.readExternal(in);
			dataLatency = in.readLong();
			transferredByteCount = in.readLong();
		}
	}
	
	@Override @SuppressWarnings("unchecked")
	public R getResult(
		final String hostAddr,
		final boolean useStorageDriverResult,
		final boolean useStorageNodeResult,
		final boolean useItemInfoResult,
		final boolean useIoTypeCodeResult,
		final boolean useStatusCodeResult,
		final boolean useReqTimeStartResult,
		final boolean useDurationResult,
		final boolean useRespLatencyResult,
		final boolean useDataLatencyResult,
		final boolean useTransferSizeResult
	) {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return (R) new BasicDataIoResult(
			useStorageDriverResult ? hostAddr : null,
			useStorageNodeResult ? nodeAddr : null,
			useItemInfoResult ? item : null,
			useIoTypeCodeResult ? ioType.ordinal() : - 1,
			useStatusCodeResult ? status.ordinal() : - 1,
			useReqTimeStartResult ? reqTimeStart : - 1,
			useDurationResult ? respTimeDone - reqTimeStart : - 1,
			useRespLatencyResult ? respTimeStart - reqTimeDone : - 1,
			useDataLatencyResult ? respDataTimeStart - reqTimeDone : - 1,
			useTransferSizeResult ? countBytesDone : -1
		);
	}

	@Override
	public void reset() {
		super.reset();
		switch(ioType) {
			case CREATE:
			case READ:
				// TODO partial read support, use rangesConfig
				try {
					contentSize = item.size();
				} catch(IOException e) {
					throw new AssertionError();
				}
				break;
			default:
				contentSize = 0;
				break;
		}
		countBytesDone = 0;
		respDataTimeStart = 0;
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
	public final boolean hasMarkedRanges() {
		return !markedRangesMaskPair[0].isEmpty() || !markedRangesMaskPair[1].isEmpty();
	}
	
	@Override
	public final BitSet[] getMarkedRangesMaskPair() {
		return markedRangesMaskPair;
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
	public final List<ByteRange> getFixedRanges() {
		return fixedRanges;
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

	@Override
	public final long getCountBytesDone() {
		return countBytesDone;
	}

	@Override
	public final void setCountBytesDone(final long n) {
		this.countBytesDone = n;
	}

	@Override
	public final long getRespDataTimeStart() {
		return respDataTimeStart;
	}

	@Override
	public final void startDataResponse() {
		respDataTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(contentSize);
		out.writeObject(fixedRanges);
		out.writeInt(randomRangesCount);
		out.writeLong(
			markedRangesMaskPair[0].isEmpty() ? 0 : markedRangesMaskPair[0].toLongArray()[0]
		);
		out.writeLong(
			markedRangesMaskPair[1].isEmpty() ? 0 : markedRangesMaskPair[1].toLongArray()[0]
		);
	}

	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		contentSrc = item.getContentSrc();
		contentSize = in.readLong();
		fixedRanges = (List<ByteRange>) in.readObject();
		randomRangesCount = in.readInt();
		markedRangesMaskPair[0].or(BitSet.valueOf(new long[] {in.readLong()}));
		markedRangesMaskPair[1].or(BitSet.valueOf(new long[] {in.readLong()}));
	}
}
