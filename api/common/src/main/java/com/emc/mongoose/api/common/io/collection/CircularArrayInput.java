package com.emc.mongoose.api.common.io.collection;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 17.08.17.
 */
public class CircularArrayInput<T>
extends ArrayInput<T> {

	public CircularArrayInput(final T[] items) {
		super(items);
	}

	/**
	 @return next data item
	 */
	@Override
	public T get()
	throws IOException {
		if(i >= size) {
			reset();
		}
		return items[i ++];
	}

	/**
	 @param buffer buffer for the data items
	 @param maxCount the count limit
	 @return the actual count of the items got in the buffer
	 @throws EOFException doesn't throw
	 */
	@Override
	public int get(final List<T> buffer, final int maxCount)
	throws EOFException, IOException {
		int n = 0;
		while(n < maxCount) {
			if(i >= size) {
				reset();
			}
			n += super.get(buffer, Math.min(size - i, maxCount - n));
		}
		return n;
	}

	@Override
	public String toString() {
		return "circularArrayInput<" + items.hashCode() + ">";
	}
}
