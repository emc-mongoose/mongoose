package com.emc.mongoose.api.common.collection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.binarySearch;

/**
 * Created on 05.05.16.
 */
public class CircularArray<T>
implements Iterable<T> {

	private final int length;
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
		if(pointer == length) {
			pointer = 0;
		}
		array[pointer++] = item;
		if(size < length) {
			size++;
		}
	}

	public int searchItem(final T item) {
		int index = binarySearch(array, pointer, size, item, arrayComparator);
		if(index < 0) {
			index = binarySearch(array, 0, pointer, item, arrayComparator);
		}
		return index;
	}

	public List<T> getLastItems(final T item) {
		final List<T> lastItems = new ArrayList<>(size);
		final int searchedItemIndex = searchItem(item);
		final Iterator<T> iterator = new LastItemsIterator(searchedItemIndex);
		while (iterator.hasNext()) {
			lastItems.add(iterator.next());
		}
		return lastItems;
	}

	public int size() {
		return size;
	}

	public Iterator<T> plainIterator() {
		return new PlainIterator();
	}

	@Override
	public Iterator<T> iterator() {
		return new LastItemsIterator();
	}

	public Iterator<T> iterator(final int startIndex) {
		return new LastItemsIterator(startIndex);
	}

	private class PlainIterator implements Iterator<T> {

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

	private class LastItemsIterator implements Iterator<T> {

		private int pointer;
		private boolean circularity = false;
		private int finishIndex;

		public LastItemsIterator() {
			this(-1);
		}

		public LastItemsIterator(final int startIndex) {
			final int arrayPrePointerIndex = CircularArray.this.pointer - 1;
			if(size < length) {
				if(startIndex < 0) {
					this.pointer = -1;
				} else {
					this.pointer = startIndex;
				}
				finishIndex = size - 1;
				return;
			}
			if(startIndex < 0) {
				this.pointer = arrayPrePointerIndex;
			} else {
				this.pointer = startIndex;
			}
			finishIndex = arrayPrePointerIndex;
			if(this.pointer >= finishIndex && startIndex != arrayPrePointerIndex) {
				circularity = true;
			}
		}

		@Override
		public boolean hasNext() {
			if(circularity) {
				if(pointer == (CircularArray.this.length - 1)) {
					pointer = -1;
					circularity = false;
				}
				return true;
			} else {
				return pointer < finishIndex;
			}
		}

		@Override
		public T next() {
			return CircularArray.this.array[++pointer];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private static final ThreadLocal<StringBuilder>
		STRING_BULDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};
	@Override
	public String toString() {
		final StringBuilder valuesBuilder = STRING_BULDER.get();
		valuesBuilder.setLength(0);
		valuesBuilder.append('[');
		if(size > 0) {
			for(int i = 0; i < size; i ++) {
				valuesBuilder.append(' ').append(this.array[i].toString()).append(", ");
			}
			valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length() - 1);
		}
		valuesBuilder.append(']');
		return valuesBuilder.toString();
	}
}
