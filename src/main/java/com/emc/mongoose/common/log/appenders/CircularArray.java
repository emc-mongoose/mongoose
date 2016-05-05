package com.emc.mongoose.common.log.appenders;

import org.apache.http.MethodNotSupportedException;

import java.util.Comparator;
import java.util.Iterator;

import static java.util.Arrays.binarySearch;

/**
 * Created on 05.05.16.
 */
public class CircularArray<T> implements Iterable<T> {

	private final int size;
	private final T[] array;
	private final Comparator<T> arrayComparator;
	// an array index on which the next item will be recorded
	private int pointer = 0;

	@SuppressWarnings("unchecked")
	public CircularArray(final int size, final Comparator<T> arrayComparator) {
		this.size = size;
		array = (T[]) new Object[size];
		this.arrayComparator = arrayComparator;
	}

	public void addItem(final T item) {
		if (pointer == size) {
			pointer = 0;
		}
		array[pointer++] = item;
	}

	public int searchItem(final T item) {
		int index = binarySearch(array, pointer, size, item, arrayComparator);
		if (index < 0) {
			index = binarySearch(array, 0, pointer, item, arrayComparator);
		}
		return index;
	}

	public int size() {
		return size;
	}

	@Override
	public Iterator<T> iterator() {
		return new CaIterator();
	}

	private class CaIterator implements Iterator<T> {

		private int pointer = 0;

		@Override
		public boolean hasNext() {
			return pointer < CircularArray.this.size;
		}

		@Override
		public T next() {
			return CircularArray.this.array[pointer++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
