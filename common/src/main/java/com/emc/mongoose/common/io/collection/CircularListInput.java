package com.emc.mongoose.common.io.collection;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

/**
 The data items input which may be get infinitely (if underlying collection allows).
 */
public class CircularListInput<T>
extends ListInput<T> {

	/**
	 @param dataItems the source data items collection
	 */
	public CircularListInput(final List<T> dataItems) {
		super(dataItems);
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
		return items.get(i ++);
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
		return "CircularListItemInput<" + items.hashCode() + ">";
	}
}
