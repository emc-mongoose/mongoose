package com.emc.mongoose.api.common.io.collection;

import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 17.08.17.
 */
public class CircularArrayOutput<T>
extends ArrayOutput<T> {

	public CircularArrayOutput(final T[] items) {
		super(items);
	}

	/**
	 @param item the data item to put
	 @throws IOException if the destination collection fails to add the data item
	 */
	@Override
	public boolean put(final T item)
	throws IOException {
		if(i >= items.length) {
			i = 0;
		}
		return super.put(item);
	}

	/**
	 Bulk circular put method
	 @param buffer the list of the items to put in a batch mode
	 @throws IOException if the destination collection fails to add the data items
	 @return the size of the buffer to put
	 */
	@Override
	public int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		//
		int n = to - from;
		if(buffer.size() > n) {
			return put(buffer.subList(from, to), 0, n);
		}
		//
		n = buffer.size();
		if(n < items.length) {
			// buffer may be placed entirely into the capacitor
			final int limit = items.length - i; // how many free space is in the capacitor;
			if(n > limit) {
				// should remove some items from the beginning of the capacitor in order to place
				// the buffer entirely
				i = n - limit;
			}
			for(int j = from; j < to; j ++) {
				items[i ++] = buffer.get(j);
			}
		} else {
			// only a tail part of the buffer may be placed into the capacitor
			i = 0; // discard all the items in the capacitor
			for(int j = n - items.length; j < n; j ++) {
				items[i ++] = buffer.get(j);
			}
		}
		return n;
	}

	/**
	 @return the corresponding input
	 @throws IOException doesn't throw
	 */
	@Override
	public CircularArrayInput<T> getInput()
	throws IOException {
		return new CircularArrayInput<>(items);
	}

	@Override
	public final String toString() {
		return "circularArrayOutput<" + items.hashCode() + ">";
	}
}
