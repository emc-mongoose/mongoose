package com.emc.mongoose.common.log.appenders.processors;

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Iterator;

class BinaryHeap<T> extends AbstractCollection<T> {

	private final static int DEFAULT_CAPACITY = 13;

	int size;
	T[] elements;
	Comparator<T> comparator;

	@SuppressWarnings("unchecked")
	BinaryHeap() {
		elements = (T[]) new Object[DEFAULT_CAPACITY + 1];
	}

	BinaryHeap(int capacity, Comparator<T> comparator) throws IllegalAccessException {
		this(capacity);
		this.comparator = comparator;
	}

	@SuppressWarnings("unchecked")
	BinaryHeap(int capacity) throws IllegalAccessException {
		if (capacity <= 0) {
			throw new IllegalAccessException("invalid capacity");
		}
		elements = (T[]) new Object[capacity + 1];
	}

	@SuppressWarnings("unchecked")
	public void clear() {
		elements = (T[]) new Object[elements.length];
		size = 0;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean isFull() {
		return elements.length == size + 1;
	}

	public void insert(T element) {
		if (isFull()) {
			grow();
		}
		percolateUpMinHeap(element);
	}

	public T peek() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		} else {
			return elements[1];
		}
	}

	public T pop() {
		final T result = peek();
		elements[1] = elements[size--];
		elements[size + 1] = null;
		if (size != 0) {
			percolateDownMinHeap(1);
		}
		return result;
	}

	protected void percolateDownMinHeap(final int index) {
		final T element = elements[index];
		int hole = index;

		while ((hole * 2) <= size) {
			int child = hole * 2;

			if (child != size && compare(elements[child + 1], elements[child]) < 0) {
				child++;
			}

			if (compare(elements[child], element) >= 0) {
				break;
			}

			elements[hole] = elements[child];
			hole = child;
		}

		elements[hole] = element;
	}

	protected void percolateUpMinHeap(final int index) {
		int hole = index;
		T element = elements[hole];
		while (hole > 1 && compare(element, elements[hole / 2]) < 0) {
			final int next = hole / 2;
			elements[hole] = elements[next];
			hole = next;
		}
		elements[hole] = element;
	}

	protected void percolateUpMinHeap(final T element) {
		elements[++size] = element;
		percolateUpMinHeap(size);
	}

	@SuppressWarnings("unchecked")
	private int compare(T a, T b) {
		if (comparator != null) {
			return comparator.compare(a, b);
		} else {
			return ((Comparable<T>) a).compareTo(b);
		}
	}

	@SuppressWarnings("unchecked")
	protected void grow() {
		final T[] elements = (T[]) new Object[this.elements.length * 2];
		System.arraycopy(elements, 0, elements, 0, elements.length);
		this.elements = elements;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append("[ ");

		for (int i = 1; i < size + 1; i++) {
			if (i != 1) {
				sb.append(", ");
			}
			sb.append(elements[i]);
		}

		sb.append(" ]");

		return sb.toString();
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int index = 1;
			private int lastReturnedIndex = -1;
			@Override
			public boolean hasNext() {
				return index <= size;
			}
			@Override
			public T next() {
				if (!hasNext()) throw new NoSuchElementException();
				lastReturnedIndex = index;
				index++;
				return elements[lastReturnedIndex];
			}
			@Override
			public void remove() {
				if (lastReturnedIndex == -1) {
					throw new IllegalStateException();
				}
				elements[lastReturnedIndex] = elements[size];
				elements[size] = null;
				size--;
				if(size != 0 && lastReturnedIndex <= size) {
					int compareToParent;
					if (lastReturnedIndex > 1) {
						compareToParent = compare(elements[lastReturnedIndex],
								elements[lastReturnedIndex / 2]);
						if (compareToParent < 0) {
							percolateUpMinHeap(lastReturnedIndex);
						} else {
							percolateDownMinHeap(lastReturnedIndex);
						}
					}
				}
				index--;
				lastReturnedIndex = -1;
			}
		};
	}

	@Override
	public boolean add(T element) {
		insert(element);
		return true;
	}

	@Override
	public int size() {
		return size;
	}
}
