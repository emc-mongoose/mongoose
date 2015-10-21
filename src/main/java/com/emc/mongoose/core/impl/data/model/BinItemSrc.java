package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;

/**
 The data item input implementation deserializing the data items from the specified stream
 */
public class BinItemSrc<T extends Item>
implements ItemSrc<T> {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	protected ObjectInputStream itemsSrc;
	protected List<T> srcBuff = null;
	protected int srcFrom = 0;
	private T lastItem = null;
	//
	public BinItemSrc(final ObjectInputStream itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	//
	public void setItemsSrc(final ObjectInputStream itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final T get()
	throws IOException {
		if(srcBuff != null && srcFrom < srcBuff.size()) {
			return srcBuff.get(srcFrom ++);
		} else {
			try {
				final Object o = itemsSrc.readUnshared();
				if(o instanceof Item) {
					return (T) o;
				} else if(o instanceof Item[]) {
					srcBuff = Arrays.asList((T[]) o);
					srcFrom = 0;
					return get();
				} else {
					throw new InvalidClassException(o == null ? null : o.getClass().getName());
				}
			} catch(final ClassNotFoundException | ClassCastException e) {
				throw new InvalidClassException(e.getMessage());
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final int get(final List<T> dstBuff, final int dstCountLimit)
	throws IOException {
		//
		if(srcBuff != null) { // there are a buffered items in the source
			final int srcCountLimit = srcBuff.size() - srcFrom;
			if(dstCountLimit < srcCountLimit) { // destination buffer has less free space than avail
				dstBuff.addAll(srcBuff.subList(srcFrom, srcFrom + dstCountLimit));
				srcFrom += dstCountLimit; // move cursor to the next position in the source buffer
				return dstCountLimit;
			} else { // destination buffer has enough free space to put all available items
				dstBuff.addAll(srcBuff.subList(srcFrom, srcFrom + srcCountLimit));
				srcBuff = null; // the buffer is sent to destination completely, dispose
				return srcCountLimit;
			}
		}
		//
		try {
			final Object o = itemsSrc.readUnshared();
			if(o instanceof Item) { // there are single item has been got from the stream
				if(dstCountLimit > 0) {
					dstBuff.add((T) o);
					return 1;
				} else {
					return 0;
				}
			} else if(o instanceof Item[]) { // there are a list of items has been got
				srcBuff = Arrays.asList((T[]) o);
				srcFrom = 0;
				return get(dstBuff, dstCountLimit);
			} else {
				throw new InvalidClassException(o == null ? null : o.getClass().getName());
			}
		} catch(final ClassNotFoundException | ClassCastException e) {
			throw new IOException(e);
		}
	}
	//
	@Override
	public void reset()
	throws IOException {
		itemsSrc.reset();
		srcBuff = null;
	}
	//
	@Override
	public T getLastItem() {
		return lastItem;
	}
	//
	@Override
	public void setLastItem(final T lastItem) {
		this.lastItem = lastItem;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void skip(final long itemsCount)
	throws IOException {
		LOG.info(Markers.MSG, ItemSrc.MSG_SKIP_START, itemsCount);
		try {
			Object o;
			long i = 0;
			while(i < itemsCount) {
				o = itemsSrc.readUnshared();
				if(o instanceof Item) {
					if(o.equals(lastItem)) {
						return;
					}
					i ++;
				} else if(o instanceof Item[]) {
					srcBuff = Arrays.asList((T[]) o);
					if(srcBuff.size() > itemsCount - i) {
						srcFrom = (int) (itemsCount - i);
						return;
					} else {
						i += srcBuff.size();
						srcBuff = null;
					}
				} else {
					throw new InvalidClassException(o == null ? null : o.getClass().getName());
				}
			}
		} catch (final ClassNotFoundException e) {
			throw new IOException(e);
		}
		LOG.info(Markers.MSG, MSG_SKIP_END);
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsSrc.close();
		srcBuff = null;
	}
	//
	@Override
	public String toString() {
		return "binItemInput<" + itemsSrc.toString() + ">";
	}
}
