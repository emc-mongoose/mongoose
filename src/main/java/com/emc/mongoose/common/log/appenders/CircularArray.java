package com.emc.mongoose.common.log.appenders;

import org.apache.http.MethodNotSupportedException;

import java.util.Comparator;
import java.util.Iterator;

import static java.util.Arrays.binarySearch;

/**
 * Created on 05.05.16.
 */
public class CircularArray<T> implements Iterable<T> {

	public final int length;
	private final T[] array;
	private final Comparator<T> arrayComparator;
	// an array index on which the next item will be recorded
	private int size = 0;
	private int pointer = 0;

	@SuppressWarnings("unchecked")
	public CircularArray(final int length, final Comparator<T> arrayComparator) {
		this.length = length;
		array = (T[]) new Object[length];
		this.arrayComparator = arrayComparator;
	}

	public void addItem(final T item) {
		if (pointer == length) {
			pointer = 0;
		}
		array[pointer++] = item;
		if (size < length) {
			size++;
		}
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

	@Override
	public String toString() {
		final StringBuilder valuesBuilder = new StringBuilder();
		valuesBuilder.append('[');
		for (T item: this) {
			valuesBuilder.append(' ').append(item).append(", ");
		}
		valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length() - 1);
		valuesBuilder.append(']');
		return valuesBuilder.toString();
	}
}
