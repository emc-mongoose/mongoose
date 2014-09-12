package com.emc.mongoose.data;
//
import com.emc.mongoose.conf.RunTimeConfig;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.Externalizable;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 22.05.14.
 */
public class Ranges
implements Map<Long, UniformData>, Externalizable {
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
	private final Map<Long, UniformData> historyMap = new ConcurrentHashMap<>();
	private BigInteger historyMask = new BigInteger(new byte[]{0});
	//
	protected final Queue<Long> pendingQueue = new ConcurrentLinkedQueue<>();
	private BigInteger pendingMask = new BigInteger(new byte[]{0});
	//
	private final long parentSize;
	private final int cellCount, cellSize;
	/**
	 @param size the size of underlying data
	 @return cell count = int(ln2(size)) + int(size^0.25)
	 @throws IllegalArgumentException if size is less than 1
	 */
	public static int getCellCount(long size)
	throws IllegalArgumentException {
		if(size>0) {
			return Long.SIZE*Byte.SIZE - Long.numberOfLeadingZeros(size) + (int) Math.pow(size, 0.25) - 1;
		} else {
			throw new IllegalArgumentException(
				String.format("The size should be more than zero, but got %d", size)
			);
		}
	}
	//
	public Ranges(final long parentSize) {
		this.parentSize = parentSize;
		cellCount = getCellCount(parentSize);
		cellSize = (int) parentSize/cellCount;
	}
	//
	public final long getPendingByteCount() {
		long sumSize = 0;
		UniformData nextRange;
		for(final long nextOffset: pendingQueue) {
			nextRange = historyMap.get(nextOffset);
			sumSize += nextRange.getSize();
		}
		return sumSize;
	}
	//
	public final int getCount() {
		return historyMap.size();
	}
	//
	public void createRandom()
	throws IllegalStateException {
		final int startCellPos = ThreadLocalRandom.current().nextInt(cellCount);
		int nextCellPos;
		for(int i = startCellPos; i< startCellPos + cellCount; i++) {
			nextCellPos = i % cellCount;
			if(!historyMask.testBit(nextCellPos) && !pendingMask.testBit(nextCellPos)) {
				pendingMask = pendingMask.setBit(nextCellPos);
				return;
			}
		}
		throw new IllegalStateException("Looks ");
	}
	//
	public void createRandom(final int count)
	throws IllegalArgumentException, IOException {
		if(count < 1 || count > parentSize) {
			throw new IllegalArgumentException(
				String.format(FMT_MSG_WRONG_RANGE_COUNT, RunTimeConfig.formatSize(parentSize))
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
		out.writeInt(historyMap.size());
		for(final long itemOffset: historyMap.keySet()) {
			out.writeLong(itemOffset);
			out.writeObject(historyMap.get(itemOffset));
		}
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		long nextOffset;
		UniformData nextRangeData;
		//
		int size = in.readInt();
		for(int i=0; i<size; i++) {
			nextOffset = in.readLong();
			nextRangeData = UniformData.class.cast(in.readObject());
			put(nextOffset, nextRangeData);
		}
	}
	//
	@Override
	public final int size() {
		return historyMap.size();
	}
	//
	@Override
	public final boolean isEmpty() {
		return historyMap.isEmpty();
	}
	//
	@Override
	public final boolean containsKey(final Object key) {
		return historyMap.containsKey(key);
	}
	//
	@Override
	public final boolean containsValue(final Object value) {
		return historyMap.containsValue(value);
	}
	//
	@Override
	public final UniformData get(final Object parentOffset) {
		return historyMap.get(parentOffset);
	}
	//
	@Override
	public final UniformData put(final Long newRangeBeg, final UniformData data)
	throws IllegalArgumentException {
		// range overlapping avoidance magic
		long oldRangeEnd, newRangeEnd = newRangeBeg + data.getSize();
		for(final long oldRangeBeg: historyMap.keySet()) {
			oldRangeEnd = oldRangeBeg + historyMap.get(oldRangeBeg).size;
			if(oldRangeBeg < newRangeBeg) { // old range begins before new one?
				if(oldRangeEnd > newRangeEnd) { // old range also ends after the end of new one?
					// stretch the new range to make it ending at the same position as old one
					data.size += oldRangeEnd - newRangeEnd;
				}
				// shrink the old range to make it not overlapping w/ new one
				historyMap.get(oldRangeBeg).size -= oldRangeEnd - newRangeBeg + 1;
			} else if(oldRangeBeg > newRangeBeg && oldRangeBeg < newRangeEnd) {
				// old range begins inside of new one, shrink new range
				data.size = oldRangeBeg - newRangeBeg - 1;
			}

		}
		//
		historyMap.put(newRangeBeg, data);
		pendingQueue.add(newRangeBeg);
		return data;
	}
	//
	@Override
	public final UniformData remove(Object parentOffset) {
		UniformData removed;
		synchronized(historyMap) {
			removed = historyMap.remove(parentOffset);
		}
		return removed;
	}
	//
	@Override
	public final void putAll(final Map<? extends Long, ? extends UniformData> map) {
		for(final long parentOffset: map.keySet()) {
			put(parentOffset, map.get(parentOffset));
		}
	}
	//
	@Override
	public final void clear() {
		historyMap.clear();
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final Set<Long> keySet() {
		return historyMap.keySet();
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final Collection<UniformData> values() {
		return historyMap.values();
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final Set<Entry<Long,UniformData>> entrySet() {
		return historyMap.entrySet();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String toString() {
		UniformData data;
		for(final long parentOffset: keySet()) {
			data = get(parentOffset);
			REQ_BUILDER.get().clear()
				.append(RunTimeConfig.LIST_SEP).append(parentOffset)
				.append(RunTimeConfig.LIST_SEP).append(data.toString());
		}
		return REQ_BUILDER.get().toString();
	}
	//
	public final Queue<Long> getPendingQueue() {
		return pendingQueue;
	}
	//
}
