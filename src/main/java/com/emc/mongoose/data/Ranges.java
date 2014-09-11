package com.emc.mongoose.data;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.logging.Markers;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 22.05.14.
 */
public class Ranges
implements Map<Long, UniformData> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static ThreadLocal<StrBuilder> REQ_BUILDER = new ThreadLocal<StrBuilder>() {
		@Override
		protected final StrBuilder initialValue() {
			return new StrBuilder();
		}
	};
	private final static String
		FMT_EXC_MSG = "Range count should be more than 0 and less than the object size = %s";
	//
	private final TreeMap<Long, UniformData> offsetMap = new TreeMap<>();
	private long parentSize;
	//
	public Ranges(final long parentSize) {
		this.parentSize = parentSize;
	}
	//
	public final long getByteCount() {
		long sumSize = 0;
		for(final UniformData data: offsetMap.values()) {
			sumSize += data.getSize();
		}
		return sumSize;
	}
	//
	public final int getCount() {
		return offsetMap.size();
	}
	//
	public void createRandom(final int count)
	throws IllegalArgumentException, IOException {
		if(count < 1 || count > parentSize) {
			throw new IllegalArgumentException(
				String.format(FMT_EXC_MSG, RunTimeConfig.formatSize(parentSize))
			);
		}
		final int spacePerRange = (int) parentSize / count;
		final ThreadLocalRandom tlr = ThreadLocalRandom.current();
		long nextItemOffset, nextRangeSize;
		for(int i = 0; i < count; i++) {
			nextItemOffset = i * spacePerRange + tlr.nextLong(spacePerRange/2);
			nextRangeSize = tlr.nextLong(1, spacePerRange/2);
			put(nextItemOffset, new UniformData(nextRangeSize));
		}
	}
	/*
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeInt(offsetMap.size());
		for(final long itemOffset: offsetMap.keySet()) {
			out.writeLong(itemOffset);
			out.writeObject(offsetMap.get(itemOffset));
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
	*/
	@Override
	public final int size() {
		return offsetMap.size();
	}
	@Override
	public final boolean isEmpty() {
		return offsetMap.isEmpty();
	}
	@Override
	public final boolean containsKey(final Object key) {
		return offsetMap.containsKey(key);
	}
	@Override
	public final boolean containsValue(final Object value) {
		return offsetMap.containsValue(value);
	}
	@Override
	public final UniformData get(final Object parentOffset) {
		return offsetMap.get(parentOffset);
	}
	@Override
	public final UniformData put(final Long parentOffset, final UniformData data)
	throws IllegalArgumentException {
		// range overlapping checks
		long existingRangeEnd, newRangeEnd = parentOffset + data.getSize();
		for(final long existingRangeOffset: offsetMap.keySet()) {
			existingRangeEnd = existingRangeOffset + offsetMap.get(existingRangeOffset).getSize();
			if(newRangeEnd < existingRangeOffset) {
				LOG.trace(Markers.MSG, "The range is completely before the existing one");
			} else if(parentOffset > existingRangeEnd) {
				LOG.trace(Markers.MSG, "The range is completely after the existing one");
			} else {
				throw new IllegalArgumentException("Range overlapping");
			}
		}
		//
		offsetMap.put(parentOffset, data);
		return data;
	}
	@Override
	public final UniformData remove(Object parentOffset) {
		UniformData removed;
		synchronized(offsetMap) {
			removed = offsetMap.remove(parentOffset);
		}
		return removed;
	}
	@Override
	public final void putAll(final Map<? extends Long, ? extends UniformData> map) {
		for(final long parentOffset: map.keySet()) {
			put(parentOffset, map.get(parentOffset));
		}
	}
	@Override
	public final void clear() {
		offsetMap.clear();
	}
	@Override @SuppressWarnings("NullableProblems")
	public final Set<Long> keySet() {
		return offsetMap.keySet();
	}
	@Override @SuppressWarnings("NullableProblems")
	public final Collection<UniformData> values() {
		return offsetMap.values();
	}
	@Override @SuppressWarnings("NullableProblems")
	public final Set<Entry<Long,UniformData>> entrySet() {
		return offsetMap.entrySet();
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
}
