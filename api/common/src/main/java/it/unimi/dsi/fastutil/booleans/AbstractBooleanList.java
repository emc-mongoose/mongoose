/* Generic definitions */

/* Assertions (useful to generate conditional code) */

/* Current type and class (and size, if applicable) */
/* Value methods */

/* Interfaces (keys) */
/* Interfaces (values) */
/* Abstract implementations (keys) */
/* Abstract implementations (values) */

/* Static containers (keys) */
/* Static containers (values) */

/* Implementations */
/* Synchronized wrappers */
/* Unmodifiable wrappers */
/* Other wrappers */

/* Methods (keys) */
/* Methods (values) */
/* Methods (keys/values) */

/* Methods that have special names depending on keys (but the special names depend on values) */

/* Equality */
/* Object/Reference-only definitions (keys) */
/* Primitive-type-only definitions (keys) */
/* Object/Reference-only definitions (values) */
/*		 
 * Copyright (C) 2002-2016 Sebastiano Vigna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package it.unimi.dsi.fastutil.booleans;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * An abstract class providing basic methods for lists implementing a
 * type-specific list interface.
 *
 * <P>
 * As an additional bonus, this class implements on top of the list operations a
 * type-specific stack.
 */

public abstract class AbstractBooleanList extends AbstractBooleanCollection
		implements
			BooleanList,
			BooleanStack {

	protected AbstractBooleanList() {
	}

	/**
	 * Ensures that the given index is nonnegative and not greater than the list
	 * size.
	 *
	 * @param index
	 *            an index.
	 * @throws IndexOutOfBoundsException
	 *             if the given index is negative or greater than the list size.
	 */
	protected void ensureIndex(final int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException("Index (" + index
					+ ") is negative");
		if (index > size())
			throw new IndexOutOfBoundsException("Index (" + index
					+ ") is greater than list size (" + (size()) + ")");
	}

	/**
	 * Ensures that the given index is nonnegative and smaller than the list
	 * size.
	 *
	 * @param index
	 *            an index.
	 * @throws IndexOutOfBoundsException
	 *             if the given index is negative or not smaller than the list
	 *             size.
	 */
	protected void ensureRestrictedIndex(final int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException("Index (" + index
					+ ") is negative");
		if (index >= size())
			throw new IndexOutOfBoundsException("Index (" + index
					+ ") is greater than or equal to list size (" + (size())
					+ ")");
	}

	public void add(final int index, final boolean k) {
		throw new UnsupportedOperationException();
	}

	public boolean add(final boolean k) {
		add(size(), k);
		return true;
	}

	public boolean removeBoolean(int i) {
		throw new UnsupportedOperationException();
	}

	public boolean set(final int index, final boolean k) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(int index, final Collection<? extends Boolean> c) {
		ensureIndex(index);
		int n = c.size();
		if (n == 0)
			return false;
		Iterator<? extends Boolean> i = c.iterator();
		while (n-- != 0)
			add(index++, i.next());
		return true;
	}

	/** Delegates to a more generic method. */
	public boolean addAll(final Collection<? extends Boolean> c) {
		return addAll(size(), c);
	}

	/** Delegates to the new covariantly stronger generic method. */

	@Deprecated
	public BooleanListIterator booleanListIterator() {
		return listIterator();
	}

	/** Delegates to the new covariantly stronger generic method. */

	@Deprecated
	public BooleanListIterator booleanListIterator(final int index) {
		return listIterator(index);
	}

	public BooleanListIterator iterator() {
		return listIterator();
	}

	public BooleanListIterator listIterator() {
		return listIterator(0);
	}

	public BooleanListIterator listIterator(final int index) {
		ensureIndex(index);

		return new AbstractBooleanListIterator() {
			int pos = index, last = -1;

			public boolean hasNext() {
				return pos < AbstractBooleanList.this.size();
			}
			public boolean hasPrevious() {
				return pos > 0;
			}
			public boolean nextBoolean() {
				if (!hasNext())
					throw new NoSuchElementException();
				return AbstractBooleanList.this.getBoolean(last = pos++);
			}
			public boolean previousBoolean() {
				if (!hasPrevious())
					throw new NoSuchElementException();
				return AbstractBooleanList.this.getBoolean(last = --pos);
			}
			public int nextIndex() {
				return pos;
			}
			public int previousIndex() {
				return pos - 1;
			}
			public void add(boolean k) {
				AbstractBooleanList.this.add(pos++, k);
				last = -1;
			}
			public void set(boolean k) {
				if (last == -1)
					throw new IllegalStateException();
				AbstractBooleanList.this.set(last, k);
			}
			public void remove() {
				if (last == -1)
					throw new IllegalStateException();
				AbstractBooleanList.this.removeBoolean(last);
				/*
				 * If the last operation was a next(), we are removing an
				 * element *before* us, and we must decrease pos
				 * correspondingly.
				 */
				if (last < pos)
					pos--;
				last = -1;
			}
		};
	}

	public boolean contains(final boolean k) {
		return indexOf(k) >= 0;
	}

	public int indexOf(final boolean k) {
		final BooleanListIterator i = listIterator();
		boolean e;
		while (i.hasNext()) {
			e = i.nextBoolean();
			if (((k) == (e)))
				return i.previousIndex();
		}
		return -1;
	}

	public int lastIndexOf(final boolean k) {
		BooleanListIterator i = listIterator(size());
		boolean e;
		while (i.hasPrevious()) {
			e = i.previousBoolean();
			if (((k) == (e)))
				return i.nextIndex();
		}
		return -1;
	}

	public void size(final int size) {
		int i = size();
		if (size > i)
			while (i++ < size)
				add((false));
		else
			while (i-- != size)
				remove(i);
	}

	public BooleanList subList(final int from, final int to) {
		ensureIndex(from);
		ensureIndex(to);
		if (from > to)
			throw new IndexOutOfBoundsException("Start index (" + from
					+ ") is greater than end index (" + to + ")");

		return new BooleanSubList(this, from, to);
	}

	/** Delegates to the new covariantly stronger generic method. */

	@Deprecated
	public BooleanList booleanSubList(final int from, final int to) {
		return subList(from, to);
	}

	/**
	 * Removes elements of this type-specific list one-by-one.
	 *
	 * <P>
	 * This is a trivial iterator-based implementation. It is expected that
	 * implementations will override this method with a more optimized version.
	 *
	 *
	 * @param from
	 *            the start index (inclusive).
	 * @param to
	 *            the end index (exclusive).
	 */

	public void removeElements(final int from, final int to) {
		ensureIndex(to);
		BooleanListIterator i = listIterator(from);
		int n = to - from;
		if (n < 0)
			throw new IllegalArgumentException("Start index (" + from
					+ ") is greater than end index (" + to + ")");
		while (n-- != 0) {
			i.nextBoolean();
			i.remove();
		}
	}

	/**
	 * Adds elements to this type-specific list one-by-one.
	 *
	 * <P>
	 * This is a trivial iterator-based implementation. It is expected that
	 * implementations will override this method with a more optimized version.
	 *
	 * @param index
	 *            the index at which to add elements.
	 * @param a
	 *            the array containing the elements.
	 * @param offset
	 *            the offset of the first element to add.
	 * @param length
	 *            the number of elements to add.
	 */

	public void addElements(int index, final boolean a[], int offset, int length) {
		ensureIndex(index);
		if (offset < 0)
			throw new ArrayIndexOutOfBoundsException("Offset (" + offset
					+ ") is negative");
		if (offset + length > a.length)
			throw new ArrayIndexOutOfBoundsException("End index ("
					+ (offset + length) + ") is greater than array length ("
					+ a.length + ")");
		while (length-- != 0)
			add(index++, a[offset++]);
	}

	public void addElements(final int index, final boolean a[]) {
		addElements(index, a, 0, a.length);
	}

	/**
	 * Copies element of this type-specific list into the given array
	 * one-by-one.
	 *
	 * <P>
	 * This is a trivial iterator-based implementation. It is expected that
	 * implementations will override this method with a more optimized version.
	 *
	 * @param from
	 *            the start index (inclusive).
	 * @param a
	 *            the destination array.
	 * @param offset
	 *            the offset into the destination array where to store the first
	 *            element copied.
	 * @param length
	 *            the number of elements to be copied.
	 */

	public void getElements(final int from, final boolean a[], int offset,
			int length) {
		BooleanListIterator i = listIterator(from);
		if (offset < 0)
			throw new ArrayIndexOutOfBoundsException("Offset (" + offset
					+ ") is negative");
		if (offset + length > a.length)
			throw new ArrayIndexOutOfBoundsException("End index ("
					+ (offset + length) + ") is greater than array length ("
					+ a.length + ")");
		if (from + length > size())
			throw new IndexOutOfBoundsException("End index (" + (from + length)
					+ ") is greater than list size (" + size() + ")");
		while (length-- != 0)
			a[offset++] = i.nextBoolean();
	}

	private boolean valEquals(final Object a, final Object b) {
		return a == null ? b == null : a.equals(b);
	}

	public boolean equals(final Object o) {
		if (o == this)
			return true;
		if (!(o instanceof List))
			return false;

		final List<?> l = (List<?>) o;
		int s = size();
		if (s != l.size())
			return false;

		if (l instanceof BooleanList) {
			final BooleanListIterator i1 = listIterator(), i2 = ((BooleanList) l)
					.listIterator();
			while (s-- != 0)
				if (i1.nextBoolean() != i2.nextBoolean())
					return false;
			return true;
		}

		final ListIterator<?> i1 = listIterator(), i2 = l.listIterator();

		while (s-- != 0)
			if (!valEquals(i1.next(), i2.next()))
				return false;

		return true;
	}

	/**
	 * Compares this list to another object. If the argument is a
	 * {@link List}, this method performs a lexicographical
	 * comparison; otherwise, it throws a <code>ClassCastException</code>.
	 *
	 * @param l
	 *            a list.
	 * @return if the argument is a {@link List}, a negative integer,
	 *         zero, or a positive integer as this list is lexicographically
	 *         less than, equal to, or greater than the argument.
	 * @throws ClassCastException
	 *             if the argument is not a list.
	 */

	public int compareTo(final List<? extends Boolean> l) {
		if (l == this)
			return 0;

		if (l instanceof BooleanList) {

			final BooleanListIterator i1 = listIterator(), i2 = ((BooleanList) l)
					.listIterator();
			int r;
			boolean e1, e2;

			while (i1.hasNext() && i2.hasNext()) {
				e1 = i1.nextBoolean();
				e2 = i2.nextBoolean();
				if ((r = (Boolean.compare((e1), (e2)))) != 0)
					return r;
			}
			return i2.hasNext() ? -1 : (i1.hasNext() ? 1 : 0);
		}

		ListIterator<? extends Boolean> i1 = listIterator(), i2 = l
				.listIterator();
		int r;

		while (i1.hasNext() && i2.hasNext()) {
			if ((r = ((Comparable<? super Boolean>) i1.next()).compareTo(i2
					.next())) != 0)
				return r;
		}
		return i2.hasNext() ? -1 : (i1.hasNext() ? 1 : 0);
	}

	/**
	 * Returns the hash code for this list, which is identical to
	 * {@link List#hashCode()}.
	 *
	 * @return the hash code for this list.
	 */
	public int hashCode() {
		BooleanIterator i = iterator();
		int h = 1, s = size();
		while (s-- != 0) {
			boolean k = i.nextBoolean();
			h = 31 * h + ((k) ? 1231 : 1237);
		}
		return h;
	}

	public void push(boolean o) {
		add(o);
	}

	public boolean popBoolean() {
		if (isEmpty())
			throw new NoSuchElementException();
		return removeBoolean(size() - 1);
	}

	public boolean topBoolean() {
		if (isEmpty())
			throw new NoSuchElementException();
		return getBoolean(size() - 1);
	}

	public boolean peekBoolean(int i) {
		return getBoolean(size() - 1 - i);
	}

	public boolean rem(boolean k) {
		int index = indexOf(k);
		if (index == -1)
			return false;
		removeBoolean(index);
		return true;
	}

	/** Delegates to <code>rem()</code>. */
	public boolean remove(final Object o) {
		return rem(((((Boolean) (o)).booleanValue())));
	}

	/** Delegates to a more generic method. */
	public boolean addAll(final int index, final BooleanCollection c) {
		return addAll(index, (Collection<? extends Boolean>) c);
	}

	/** Delegates to a more generic method. */
	public boolean addAll(final int index, final BooleanList l) {
		return addAll(index, (BooleanCollection) l);
	}

	public boolean addAll(final BooleanCollection c) {
		return addAll(size(), c);
	}

	public boolean addAll(final BooleanList l) {
		return addAll(size(), l);
	}

	/** Delegates to the corresponding type-specific method. */
	public void add(final int index, final Boolean ok) {
		add(index, ok.booleanValue());
	}

	/**
	 * Delegates to the corresponding type-specific method.
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	public Boolean set(final int index, final Boolean ok) {
		return (Boolean.valueOf(set(index, ok.booleanValue())));
	}

	/**
	 * Delegates to the corresponding type-specific method.
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	public Boolean get(final int index) {
		return (Boolean.valueOf(getBoolean(index)));
	}

	/** Delegates to the corresponding type-specific method. */
	public int indexOf(final Object ok) {
		return indexOf(((((Boolean) (ok)).booleanValue())));
	}

	/** Delegates to the corresponding type-specific method. */
	public int lastIndexOf(final Object ok) {
		return lastIndexOf(((((Boolean) (ok)).booleanValue())));
	}

	/**
	 * Delegates to the corresponding type-specific method.
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	public Boolean remove(final int index) {
		return (Boolean.valueOf(removeBoolean(index)));
	}

	/** Delegates to the corresponding type-specific method. */
	public void push(Boolean o) {
		push(o.booleanValue());
	}

	/**
	 * Delegates to the corresponding type-specific method.
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	public Boolean pop() {
		return Boolean.valueOf(popBoolean());
	}

	/**
	 * Delegates to the corresponding type-specific method.
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	public Boolean top() {
		return Boolean.valueOf(topBoolean());
	}

	/**
	 * Delegates to the corresponding type-specific method.
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	public Boolean peek(int i) {
		return Boolean.valueOf(peekBoolean(i));
	}

	public String toString() {
		final StringBuilder s = new StringBuilder();
		final BooleanIterator i = iterator();
		int n = size();
		boolean k;
		boolean first = true;

		s.append("[");

		while (n-- != 0) {
			if (first)
				first = false;
			else
				s.append(", ");
			k = i.nextBoolean();

			s.append(String.valueOf(k));
		}

		s.append("]");
		return s.toString();
	}

	public static class BooleanSubList extends AbstractBooleanList
			implements
				java.io.Serializable {
		private static final long serialVersionUID = -7046029254386353129L;
		/** The list this sublist restricts. */
		protected final BooleanList l;
		/** Initial (inclusive) index of this sublist. */
		protected final int from;
		/** Final (exclusive) index of this sublist. */
		protected int to;

		private static final boolean ASSERTS = false;

		public BooleanSubList(final BooleanList l, final int from, final int to) {
			this.l = l;
			this.from = from;
			this.to = to;
		}

		private void assertRange() {
			if (ASSERTS) {
				assert from <= l.size();
				assert to <= l.size();
				assert to >= from;
			}
		}

		public boolean add(final boolean k) {
			l.add(to, k);
			to++;
			if (ASSERTS)
				assertRange();
			return true;
		}

		public void add(final int index, final boolean k) {
			ensureIndex(index);
			l.add(from + index, k);
			to++;
			if (ASSERTS)
				assertRange();
		}

		public boolean addAll(final int index,
				final Collection<? extends Boolean> c) {
			ensureIndex(index);
			to += c.size();
			if (ASSERTS) {
				boolean retVal = l.addAll(from + index, c);
				assertRange();
				return retVal;
			}
			return l.addAll(from + index, c);
		}

		public boolean getBoolean(int index) {
			ensureRestrictedIndex(index);
			return l.getBoolean(from + index);
		}

		public boolean removeBoolean(int index) {
			ensureRestrictedIndex(index);
			to--;
			return l.removeBoolean(from + index);
		}

		public boolean set(int index, boolean k) {
			ensureRestrictedIndex(index);
			return l.set(from + index, k);
		}

		public void clear() {
			removeElements(0, size());
			if (ASSERTS)
				assertRange();
		}

		public int size() {
			return to - from;
		}

		public void getElements(final int from, final boolean[] a,
				final int offset, final int length) {
			ensureIndex(from);
			if (from + length > size())
				throw new IndexOutOfBoundsException("End index (" + from
						+ length + ") is greater than list size (" + size()
						+ ")");
			l.getElements(this.from + from, a, offset, length);
		}

		public void removeElements(final int from, final int to) {
			ensureIndex(from);
			ensureIndex(to);
			l.removeElements(this.from + from, this.from + to);
			this.to -= (to - from);
			if (ASSERTS)
				assertRange();
		}

		public void addElements(int index, final boolean a[], int offset,
				int length) {
			ensureIndex(index);
			l.addElements(this.from + index, a, offset, length);
			this.to += length;
			if (ASSERTS)
				assertRange();
		}

		public BooleanListIterator listIterator(final int index) {
			ensureIndex(index);

			return new AbstractBooleanListIterator() {
				int pos = index, last = -1;

				public boolean hasNext() {
					return pos < size();
				}
				public boolean hasPrevious() {
					return pos > 0;
				}
				public boolean nextBoolean() {
					if (!hasNext())
						throw new NoSuchElementException();
					return l.getBoolean(from + (last = pos++));
				}
				public boolean previousBoolean() {
					if (!hasPrevious())
						throw new NoSuchElementException();
					return l.getBoolean(from + (last = --pos));
				}
				public int nextIndex() {
					return pos;
				}
				public int previousIndex() {
					return pos - 1;
				}
				public void add(boolean k) {
					if (last == -1)
						throw new IllegalStateException();
					BooleanSubList.this.add(pos++, k);
					last = -1;
					if (ASSERTS)
						assertRange();
				}
				public void set(boolean k) {
					if (last == -1)
						throw new IllegalStateException();
					BooleanSubList.this.set(last, k);
				}
				public void remove() {
					if (last == -1)
						throw new IllegalStateException();
					BooleanSubList.this.removeBoolean(last);
					/*
					 * If the last operation was a next(), we are removing an
					 * element *before* us, and we must decrease pos
					 * correspondingly.
					 */
					if (last < pos)
						pos--;
					last = -1;
					if (ASSERTS)
						assertRange();
				}
			};
		}

		public BooleanList subList(final int from, final int to) {
			ensureIndex(from);
			ensureIndex(to);
			if (from > to)
				throw new IllegalArgumentException("Start index (" + from
						+ ") is greater than end index (" + to + ")");

			return new BooleanSubList(this, from, to);
		}

		public boolean rem(boolean k) {
			int index = indexOf(k);
			if (index == -1)
				return false;
			to--;
			l.removeBoolean(from + index);
			if (ASSERTS)
				assertRange();
			return true;
		}

		public boolean remove(final Object o) {
			return rem(((((Boolean) (o)).booleanValue())));
		}

		public boolean addAll(final int index, final BooleanCollection c) {
			ensureIndex(index);
			to += c.size();
			if (ASSERTS) {
				boolean retVal = l.addAll(from + index, c);
				assertRange();
				return retVal;
			}
			return l.addAll(from + index, c);
		}

		public boolean addAll(final int index, final BooleanList l) {
			ensureIndex(index);
			to += l.size();
			if (ASSERTS) {
				boolean retVal = this.l.addAll(from + index, l);
				assertRange();
				return retVal;
			}
			return this.l.addAll(from + index, l);
		}
	}

}
