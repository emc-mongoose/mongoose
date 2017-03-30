package com.emc.mongoose.common.io.collection;

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

	@Override
	public int get(final List<T> buffer, final int maxCount)
	throws IOException {
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
