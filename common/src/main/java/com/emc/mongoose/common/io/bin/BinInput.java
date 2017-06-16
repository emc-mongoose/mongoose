package com.emc.mongoose.common.io.bin;

import com.emc.mongoose.common.io.Input;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.util.List;

/**
 The data item input implementation deserializing the data items from the specified stream
 */
public class BinInput<T>
implements Input<T> {
	
	protected ObjectInputStream itemsSrc;
	protected T[] srcBuff = null;
	protected int srcBuffPos = 0;
	private T lastItem = null;
	
	public BinInput(final ObjectInputStream itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	
	public void setItemsSrc(final ObjectInputStream itemsSrc) {
		this.itemsSrc = itemsSrc;
	}
	
	@Override @SuppressWarnings("unchecked")
	public final T get()
	throws IOException {
		if(srcBuff != null && srcBuffPos < srcBuff.length) {
			return srcBuff[srcBuffPos ++];
		} else {
			try {
				final Object o = itemsSrc.readUnshared();
				if(o instanceof Object[]) {
					srcBuff = (T[]) o;
					srcBuffPos = 0;
					return get();
				} else {
					return (T) o;
				}
			} catch(final ClassNotFoundException | ClassCastException e) {
				throw new InvalidClassException(e.getMessage());
			}
		}
	}
	
	@Override @SuppressWarnings("unchecked")
	public final int get(final List<T> dstBuff, final int dstCountLimit)
	throws IOException {
		
		if(srcBuff != null) { // there are a buffered items in the source
			final int srcCountLimit = srcBuff.length - srcBuffPos;
			if(dstCountLimit < srcCountLimit) { // destination buffer has less free space than avail
				for(int i = srcBuffPos; i < srcBuffPos + dstCountLimit; i ++) {
					dstBuff.add(srcBuff[i]);
				}
				srcBuffPos += dstCountLimit; // move cursor to the next position in the source buffer
				return dstCountLimit;
			} else { // destination buffer has enough free space to put all available items
				for(int i = srcBuffPos; i < srcBuffPos + srcCountLimit; i ++) {
					dstBuff.add(srcBuff[i]);
				}
				srcBuff = null; // the buffer is sent to destination completely, dispose
				return srcCountLimit;
			}
		}
		
		try {
			final Object o = itemsSrc.readUnshared();
			if(o instanceof Object[]) { // there are a list of items has been got
				srcBuff = (T[]) o;
				srcBuffPos = 0;
				return get(dstBuff, dstCountLimit);
			} else { // there are single item has been got from the stream
				if(dstCountLimit > 0) {
					dstBuff.add((T) o);
					return 1;
				} else {
					return 0;
				}
			}
		} catch(final ClassNotFoundException | ClassCastException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public void reset()
	throws IOException {
		itemsSrc.reset();
		srcBuff = null;
	}
	
	@Override @SuppressWarnings("unchecked")
	public long skip(final long itemsCount)
	throws IOException {
		try {
			Object o;
			long i = 0;
			while(i < itemsCount) {
				o = itemsSrc.readUnshared();
				if(o instanceof Object[]) {
					srcBuff = (T[]) o;
					if(srcBuff.length > itemsCount - i) {
						srcBuffPos = (int) (itemsCount - i);
						break;
					} else {
						i += srcBuff.length;
						srcBuff = null;
					}
				} else {
					if(o.equals(lastItem)) {
						break;
					}
					i ++;
				}
			}
			return i;
		} catch(final ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public void close()
	throws IOException {
		itemsSrc.close();
		srcBuff = null;
	}
	
	@Override
	public String toString() {
		return "binInput<" + itemsSrc.toString() + ">";
	}
}
