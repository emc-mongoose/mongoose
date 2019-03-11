package com.emc.mongoose.base.item.op.data;

import static com.github.akurilov.commons.system.SizeInBytes.formatFixedSize;
import static java.lang.System.nanoTime;

import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.OperationImpl;
import com.emc.mongoose.base.storage.Credential;
import com.github.akurilov.commons.collection.Range;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** Created by andrey on 25.09.16. */
public class DataOperationImpl<T extends DataItem> extends OperationImpl<T>
				implements DataOperation<T> {

	protected final BitSet[] markedRangesMaskPair = new BitSet[]{new BitSet(Long.SIZE), new BitSet(Long.SIZE)
	};
	private int randomRangesCount = 0;
	private List<Range> fixedRanges = null;
	private List<T> srcItemsToConcat = null;
	protected long contentSize = 0;

	protected volatile DataInput dataInput = null;
	protected volatile long countBytesDone = 0;
	protected volatile long respDataTimeStart = 0;
	private volatile DataItem currRange = null;
	private volatile int currRangeIdx = 0;

	public DataOperationImpl() {
		super();
	}

	public DataOperationImpl(
					final int originIndex,
					final OpType opType,
					final T item,
					final String srcPath,
					final String dstPath,
					final Credential credential,
					final List<Range> fixedRanges,
					final int randomRangesCount)
					throws IllegalArgumentException {
		super(originIndex, opType, item, srcPath, dstPath, credential);
		this.fixedRanges = fixedRanges;
		this.randomRangesCount = randomRangesCount;
		reset();
		dataInput = item.dataInput();
	}

	public DataOperationImpl(
					final int originIndex,
					final OpType opType,
					final T item,
					final String srcPath,
					final String dstPath,
					final Credential credential,
					final List<Range> fixedRanges,
					final int randomRangesCount,
					final List<T> srcItemsToConcat)
					throws IllegalArgumentException {
		this(originIndex, opType, item, srcPath, dstPath, credential, fixedRanges, randomRangesCount);
		this.srcItemsToConcat = srcItemsToConcat;
	}

	protected DataOperationImpl(final DataOperationImpl<T> other) {
		super(other);
		this.contentSize = other.contentSize;
		this.randomRangesCount = other.randomRangesCount;
		this.fixedRanges = other.fixedRanges;
		this.srcItemsToConcat = other.srcItemsToConcat;
		this.countBytesDone = other.countBytesDone;
		this.respDataTimeStart = other.respDataTimeStart;
	}

	@Override
	public DataOperationImpl<T> result() {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return new DataOperationImpl<>(this);
	}

	@Override
	public void reset() throws IllegalArgumentException {

		super.reset();

		countBytesDone = 0;
		respDataTimeStart = 0;
		currRange = null;
		currRangeIdx = 0;
		markedRangesMaskPair[0].clear();
		markedRangesMaskPair[1].clear();

		try {
			switch (opType) {
			case CREATE:
				contentSize = item.size();
				break;
			case READ:
				if (fixedRanges == null || fixedRanges.isEmpty()) {
					if (randomRangesCount > 0) {
						markRandomRanges(randomRangesCount);
						contentSize = markedRangesSize();
					} else {
						// read the entire data item
						contentSize = item.size();
					}
				} else {
					contentSize = markedRangesSize();
					if (contentSize > item.size()) {
						throw new IllegalArgumentException(
										"Fixed ranges size ("
														+ formatFixedSize(contentSize)
														+ ") "
														+ "is more than data item size ("
														+ formatFixedSize(item.size()));
					}
				}
				break;
			case UPDATE:
				if (fixedRanges == null || fixedRanges.isEmpty()) {
					if (randomRangesCount > 0) {
						markRandomRanges(randomRangesCount);
					} else {
						// overwrite the entire data item
						fixedRanges = new ArrayList<>(1);
						fixedRanges.add(new Range(0L, item.size() - 1, -1));
					}
				}
				contentSize = markedRangesSize();
				break;
			default:
				contentSize = 0;
				break;
			}
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public final void markRandomRanges(final int count) {
		try {
			final int countRangesTotal = DataItem.rangeCount(item.size());
			if (count < 1 || count > countRangesTotal) {
				throw new AssertionError(
								"Range count should be more than 0 and less than max "
												+ countRangesTotal
												+ " for the item size");
			}
			for (int i = 0; i < count; i++) {
				markRandomRangesActually(countRangesTotal);
			}
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
	}

	private void markRandomRangesActually(final int countRangesTotal) {
		final int startCellPos = (int) (nanoTime() % countRangesTotal);
		int nextCellPos;
		if (countRangesTotal > item.updatedRangesCount() + markedRangesMaskPair[0].cardinality()) {
			// current layer has not updated yet ranges
			for (int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if (!item.isRangeUpdated(nextCellPos)) {
					if (!markedRangesMaskPair[0].get(nextCellPos)) {
						markedRangesMaskPair[0].set(nextCellPos);
						break;
					}
				}
			}
		} else {
			// update the next layer ranges
			for (int i = 0; i < countRangesTotal; i++) {
				nextCellPos = (startCellPos + i) % countRangesTotal;
				if (!markedRangesMaskPair[0].get(nextCellPos)) {
					if (!markedRangesMaskPair[1].get(nextCellPos)) {
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
	public final BitSet[] markedRangesMaskPair() {
		return markedRangesMaskPair;
	}

	@Override
	public final long markedRangesSize() {
		long sumSize = 0;
		if (fixedRanges == null || fixedRanges.isEmpty()) {
			try {
				for (int i = 0; i < DataItem.rangeCount(item.size()); i++) {
					if (markedRangesMaskPair[0].get(i) || markedRangesMaskPair[1].get(i)) {
						sumSize += item.rangeSize(i);
					}
				}
			} catch (final IOException e) {
				throw new AssertionError(e);
			}
		} else {
			long nextBeg, nextEnd, nextSize;
			for (final Range nextRange : fixedRanges) {
				nextBeg = nextRange.getBeg();
				nextEnd = nextRange.getEnd();
				nextSize = nextRange.getSize();
				if (nextSize == -1) {
					if (nextBeg == -1) {
						sumSize += nextEnd;
					} else if (nextEnd == -1) {
						try {
							sumSize += item.size() - nextBeg;
						} catch (final IOException e) {
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
	public final List<Range> fixedRanges() {
		return fixedRanges;
	}

	@Override
	public final int randomRangesCount() {
		return randomRangesCount;
	}

	@Override
	public final List<T> srcItemsToConcat() {
		return srcItemsToConcat;
	}

	@Override
	public final int currRangeIdx() {
		return currRangeIdx;
	}

	@Override
	public final void currRangeIdx(final int currRangeIdx) {
		currRange = null;
		this.currRangeIdx = currRangeIdx;
	}

	@Override
	public final DataItem currRange() {
		try {
			if (currRange == null && currRangeIdx < DataItem.rangeCount(item.size())) {
				final long currRangeSize = item.rangeSize(currRangeIdx);
				final long currRangeOffset = DataItem.rangeOffset(currRangeIdx);
				final int layerIdx = item.layer();
				currRange = item.slice(currRangeOffset, currRangeSize);
				if (item.isRangeUpdated(currRangeIdx)) {
					currRange.layer(layerIdx + 1);
				}
			}
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
		return currRange;
	}

	@Override
	public final DataItem currRangeUpdate() {
		if (currRange == null) {
			final int layerIdx = item.layer();
			if (markedRangesMaskPair[0].get(currRangeIdx)) {
				final long currRangeSize = item.rangeSize(currRangeIdx);
				final long currRangeOffset = DataItem.rangeOffset(currRangeIdx);
				currRange = item.slice(currRangeOffset, currRangeSize);
				currRange.layer(layerIdx + 1);
			} else if (markedRangesMaskPair[1].get(currRangeIdx)) {
				final long currRangeSize = item.rangeSize(currRangeIdx);
				final long currRangeOffset = DataItem.rangeOffset(currRangeIdx);
				currRange = item.slice(currRangeOffset, currRangeSize);
				currRange.layer(layerIdx + 2);
			} else {
				currRange = null;
			}
		}
		return currRange;
	}

	@Override
	public final long countBytesDone() {
		return countBytesDone;
	}

	@Override
	public final void countBytesDone(final long n) {
		this.countBytesDone = n;
	}

	@Override
	public final long respDataTimeStart() {
		return respDataTimeStart;
	}

	@Override
	public final void startDataResponse() {
		respDataTimeStart = Operation.START_OFFSET_MICROS + nanoTime() / 1000;
		if (reqTimeDone == 0) {
			throw new IllegalStateException(
							"Response data is started ("
											+ respDataTimeStart
											+ ") before the request is finished ("
											+ reqTimeDone
											+ ")");
		}
	}

	@Override
	public final long dataLatency() {
		return respDataTimeStart - reqTimeDone;
	}
}
