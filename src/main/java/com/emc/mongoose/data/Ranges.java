package com.emc.mongoose.data;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.logging.Markers;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 22.05.14.
 */
public class Ranges
implements Externalizable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static ThreadLocal<StrBuilder> REQ_BUILDER = new ThreadLocal<StrBuilder>() {
		@Override
		protected final StrBuilder initialValue() {
			return new StrBuilder();
		}
	};
	private final static String FMT_MSG_WRONG_RANGE_COUNT =
		"Range count should be more than 0 and less than the object size = %s";
	//
	private volatile BigInteger
		historyMask = BigInteger.ZERO,
		pendingMask = BigInteger.ZERO;
	private final Map<Long,UniformData>
		historyCache = new ConcurrentHashMap<>(),
		pendingCache = new ConcurrentHashMap<>();
	//
	private UniformData parentData;
	private int cellCount, cellSize;
	/**
	 @param size the size of underlying data
	 @return cell count = int(ln2(size)) + int(size^0.25)
	 @throws IllegalArgumentException if size is less than 1
	 */
	public static int calcCellCount(long size)
	throws IllegalArgumentException {
		if(size > 0) {
			return Long.SIZE - Long.numberOfLeadingZeros(size) - 1 + (int) Math.pow(size, 0.125);
		} else {
			throw new IllegalArgumentException(
				String.format("The size should be more than zero, but got %d", size)
			);
		}
	}
	//
	protected Ranges(final UniformData parentData) {
		this.parentData = parentData;
		cellCount = calcCellCount(parentData.size);
		LOG.trace(
			Markers.MSG, "Calculated cell count {} for item of size {}",
			cellCount, RunTimeConfig.formatSize(parentData.size)
		);
		cellSize = (int) parentData.size/cellCount;
	}
	//
	Ranges(final UniformData parentData, final BigInteger historyMask) {
		this(parentData);
		this.historyMask = historyMask;
	}
	//
	public final int getCellCount() {
		return cellCount;
	}
	//
	public final long getPendingByteCount() {
		long sumSize = 0;
		for(int i=0; i<cellCount; i++) {
			if(pendingMask.testBit(i)) {
				sumSize += cellSize;
			}
		}
		return sumSize;
	}
	//
	public final int getCount() {
		int count = 0;
		for(int i=0; i<cellCount; i++) {
			if(pendingMask.testBit(i) || pendingMask.testBit(i)) {
				count++;
			}
		}
		return count;
	}
	//
	public final List<Long> getPendingRangeOffsets() {
		final List<Long> pendingRangeOffsets = new LinkedList<>();
		for(int i=0; i<cellCount; i++) {
			if(pendingMask.testBit(i)) {
				pendingRangeOffsets.add((long)(i)*cellSize);
			}
		}
		return pendingRangeOffsets;
	}
	//
	public final List<Long> getHistoryRangeOffsets() {
		final List<Long> historyRangeOffsets = new LinkedList<>();
		long nextOffset;
		for(int i=0; i<cellCount; i++) {
			if(historyMask.testBit(i)) {
				nextOffset = i * cellSize;
				getRangeData(nextOffset); // pregenerate range data if not exists in cache
				historyRangeOffsets.add(nextOffset);
			}
		}
		return historyRangeOffsets;
	}
	//
	public final BigInteger getHistoryMask() {
		return historyMask;
	}
	//
	public final BigInteger getPendingMask() {
		return pendingMask;
	}
	//
	public final UniformData getRangeData(final long offset) {
		UniformData rangeData = null;
		if(pendingCache.containsKey(offset)) {
			rangeData = pendingCache.get(offset);
		} else if(historyMask.testBit((int)offset/cellSize)) {
			if(historyCache.containsKey(offset)) {
				rangeData = historyCache.get(offset);
			} else {
				rangeData = new UniformData(
					parentData.offset + offset, cellSize, UniformDataSource.DATA_SRC_UPDATE
				);
				historyCache.put(offset, rangeData);
			}
		}
		return rangeData;
	}
	//
	public final boolean wasUpdated(final int cellPos) {
		return historyMask.testBit(cellPos);
	}
	//
	public final void createRandom()
	throws IllegalStateException {
		final int startCellPos = ThreadLocalRandom.current().nextInt(cellCount);
		int nextCellPos;
		long nextOffset;
		for(int i=startCellPos; i<startCellPos+cellCount; i++) {
			nextCellPos = i % cellCount;
			if(!historyMask.testBit(nextCellPos) && !pendingMask.testBit(nextCellPos)) {
				pendingMask = pendingMask.setBit(nextCellPos);
				nextOffset = nextCellPos * cellSize;
				LOG.trace(
					Markers.MSG, "Update cell at position: {}, offset: {}, new mask: {}",
					nextCellPos, nextCellPos, pendingMask.toString(0x10)
				);
				pendingCache.put(
					nextOffset,
					new UniformData(
						parentData.offset + nextOffset, cellSize, UniformDataSource.DATA_SRC_UPDATE
					)
				);
				return;
			}
		}
		throw new IllegalStateException("Looks like there's no free range to update");
	}
	//
	public final void createRandom(final int count)
	throws IllegalArgumentException, IllegalStateException {
		if(count < 1 || count > cellCount) {
			throw new IllegalArgumentException(
				String.format(FMT_MSG_WRONG_RANGE_COUNT, RunTimeConfig.formatSize(cellCount))
			);
		}
		for(int i = 0; i < count; i++) {
			createRandom();
		}
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeLong(parentData.size);
		out.writeObject(historyMask);
		out.writeObject(pendingMask);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		parentData.size = in.readLong();
		cellCount = calcCellCount(parentData.size);
		cellSize = (int) parentData.size / cellCount;
		historyMask = BigInteger.class.cast(in.readObject());
		pendingMask = BigInteger.class.cast(in.readObject());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String toString() {
		return historyMask.toString(0x10);
	}
	//
	public final void fromString(final String v) {
		historyMask = new BigInteger(v, 0x10);
		pendingMask = BigInteger.ZERO;
	}
	//
	public final synchronized void movePendingToHistory() {
		LOG.trace(Markers.MSG, "History mask before: {}", historyMask.toString(0x10));
		LOG.trace(Markers.MSG, "Pending mask before: {}", pendingMask.toString(0x10));
		historyMask = historyMask.add(pendingMask);
		LOG.trace(Markers.MSG, "History mask after: {}", historyMask.toString(0x10));
		for(final long offset: pendingCache.keySet()) {
			if(historyCache.containsKey(offset)) {
				LOG.debug(Markers.ERR, "Updated ranges history already contains the pending range");
			} else {
				historyCache.put(offset, pendingCache.remove(offset));
			}
		}
		pendingMask = BigInteger.ZERO;
	}
}
