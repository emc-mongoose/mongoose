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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
/**
 Created by kurila on 22.05.14.
 */
public class Ranges
implements Externalizable, Map<Long, UniformData> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static ThreadLocal<StrBuilder> REQ_BUILDER = new ThreadLocal<StrBuilder>() {
		@Override
		protected final StrBuilder initialValue() {
			return new StrBuilder();
		}
	};
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
		final String excMsg = "Range count should be more than 0 and less than the object size = " +
			Long.toString(parentSize);
		if(count < 1 || count > parentSize) {
			throw new IllegalArgumentException(excMsg);
		}
		final int spacePerRange = (int) parentSize / count;
		final ThreadLocalRandom tlr = ThreadLocalRandom.current();
		long nextItemOffset, nextRangeSize;
		for(int i = 0; i < count; i++) {
			nextItemOffset = i * spacePerRange + tlr.nextLong(spacePerRange);
			nextRangeSize = tlr.nextLong(1, spacePerRange);
			put(nextItemOffset, new UniformData(nextRangeSize));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
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
	////////////////////////////////////////////////////////////////////////////////////////////////
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
			if(parentOffset < existingRangeOffset && newRangeEnd <= existingRangeOffset) {
				LOG.trace(Markers.MSG, "The range is completely before the existing one");
			} else if(parentOffset > existingRangeOffset && existingRangeEnd >= parentOffset) {
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
		UniformData removed, nextData;
		synchronized(offsetMap) {
			removed = offsetMap.remove(parentOffset);
			for(final long nextParentOffset : offsetMap.keySet()) {
				nextData = offsetMap.get(nextParentOffset);
			}
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
	public final void fromString(final String v)
	throws IllegalArgumentException, NullPointerException {
		clear();
		final String vv[] = v.split(RunTimeConfig.LIST_SEP);
		UniformData nextRangeData;
		if(vv.length > 0 && vv.length%3==0) {
			for(int i=0; i<vv.length; i+=3) {
				nextRangeData = new UniformData(
					Long.parseLong(vv[i+1], 0x10), Long.parseLong(vv[i+2], 0x10)
				);
				put(Long.parseLong(vv[i], 0x10), nextRangeData);
			}
		} else {
			throw new IllegalArgumentException("Invalid range metainfo: "+v);
		}
	}
	//
}
