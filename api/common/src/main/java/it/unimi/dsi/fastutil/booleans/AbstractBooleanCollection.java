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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * An abstract class providing basic methods for collections implementing a
 * type-specific interface.
 *
 * <P>
 * In particular, this class provide {@link #iterator()}, <code>add()</code>,
 * {@link #remove(Object)} and {@link #contains(Object)} methods that just call
 * the type-specific counterpart.
 */

public abstract class AbstractBooleanCollection
		extends
			AbstractCollection<Boolean> implements BooleanCollection {

	protected AbstractBooleanCollection() {
	}

	public boolean[] toArray(boolean a[]) {
		return toBooleanArray(a);
	}

	public boolean[] toBooleanArray() {
		return toBooleanArray(null);
	}

	public boolean[] toBooleanArray(boolean a[]) {
		if (a == null || a.length < size())
			a = new boolean[size()];
		BooleanIterators.unwrap(iterator(), a);
		return a;
	}

	/**
	 * Adds all elements of the given type-specific collection to this
	 * collection.
	 *
	 * @param c
	 *            a type-specific collection.
	 * @return <code>true</code> if this collection changed as a result of the
	 *         call.
	 */

	public boolean addAll(BooleanCollection c) {
		boolean retVal = false;
		final BooleanIterator i = c.iterator();
		int n = c.size();

		while (n-- != 0)
			if (add(i.nextBoolean()))
				retVal = true;
		return retVal;
	}

	/**
	 * Checks whether this collection contains all elements from the given
	 * type-specific collection.
	 *
	 * @param c
	 *            a type-specific collection.
	 * @return <code>true</code> if this collection contains all elements of the
	 *         argument.
	 */

	public boolean containsAll(BooleanCollection c) {
		final BooleanIterator i = c.iterator();
		int n = c.size();

		while (n-- != 0)
			if (!contains(i.nextBoolean()))
				return false;

		return true;
	}

	/**
	 * Retains in this collection only elements from the given type-specific
	 * collection.
	 *
	 * @param c
	 *            a type-specific collection.
	 * @return <code>true</code> if this collection changed as a result of the
	 *         call.
	 */

	public boolean retainAll(BooleanCollection c) {
		boolean retVal = false;
		int n = size();

		final BooleanIterator i = iterator();

		while (n-- != 0) {
			if (!c.contains(i.nextBoolean())) {
				i.remove();
				retVal = true;
			}
		}

		return retVal;
	}

	/**
	 * Remove from this collection all elements in the given type-specific
	 * collection.
	 *
	 * @param c
	 *            a type-specific collection.
	 * @return <code>true</code> if this collection changed as a result of the
	 *         call.
	 */

	public boolean removeAll(BooleanCollection c) {
		boolean retVal = false;
		int n = c.size();

		final BooleanIterator i = c.iterator();

		while (n-- != 0)
			if (rem(i.nextBoolean()))
				retVal = true;

		return retVal;
	}

	public Object[] toArray() {
		final Object[] a = new Object[size()];
		it.unimi.dsi.fastutil.objects.ObjectIterators.unwrap(iterator(), a);
		return a;
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		final int size = size();
		if (a.length < size)
			a = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
					.getComponentType(), size);
		it.unimi.dsi.fastutil.objects.ObjectIterators.unwrap(iterator(), a);
		if (size < a.length)
			a[size] = null;
		return a;
	}

	/**
	 * Adds all elements of the given collection to this collection.
	 *
	 * @param c
	 *            a collection.
	 * @return <code>true</code> if this collection changed as a result of the
	 *         call.
	 */

	public boolean addAll(Collection<? extends Boolean> c) {
		boolean retVal = false;
		final Iterator<? extends Boolean> i = c.iterator();
		int n = c.size();

		while (n-- != 0)
			if (add(i.next()))
				retVal = true;
		return retVal;
	}

	public boolean add(boolean k) {
		throw new UnsupportedOperationException();
	}

	/** Delegates to the new covariantly stronger generic method. */

	@Deprecated
	public BooleanIterator booleanIterator() {
		return iterator();
	}

	public abstract BooleanIterator iterator();

	/** Delegates to the type-specific <code>rem()</code> method. */
	public boolean remove(Object ok) {
		if (ok == null)
			return false;
		return rem(((((Boolean) (ok)).booleanValue())));
	}

	/** Delegates to the corresponding type-specific method. */
	public boolean add(final Boolean o) {
		return add(o.booleanValue());
	}

	/** Delegates to the corresponding type-specific method. */
	public boolean rem(final Object o) {
		if (o == null)
			return false;
		return rem(((((Boolean) (o)).booleanValue())));
	}

	/** Delegates to the corresponding type-specific method. */
	public boolean contains(final Object o) {
		if (o == null)
			return false;
		return contains(((((Boolean) (o)).booleanValue())));
	}

	public boolean contains(final boolean k) {
		final BooleanIterator iterator = iterator();
		while (iterator.hasNext())
			if (k == iterator.nextBoolean())
				return true;
		return false;
	}

	public boolean rem(final boolean k) {
		final BooleanIterator iterator = iterator();
		while (iterator.hasNext())
			if (k == iterator.nextBoolean()) {
				iterator.remove();
				return true;
			}
		return false;
	}

	/**
	 * Checks whether this collection contains all elements from the given
	 * collection.
	 *
	 * @param c
	 *            a collection.
	 * @return <code>true</code> if this collection contains all elements of the
	 *         argument.
	 */

	public boolean containsAll(Collection<?> c) {
		int n = c.size();

		final Iterator<?> i = c.iterator();
		while (n-- != 0)
			if (!contains(i.next()))
				return false;

		return true;
	}

	/**
	 * Retains in this collection only elements from the given collection.
	 *
	 * @param c
	 *            a collection.
	 * @return <code>true</code> if this collection changed as a result of the
	 *         call.
	 */

	public boolean retainAll(Collection<?> c) {
		boolean retVal = false;
		int n = size();

		final Iterator<?> i = iterator();
		while (n-- != 0) {
			if (!c.contains(i.next())) {
				i.remove();
				retVal = true;
			}
		}

		return retVal;
	}

	/**
	 * Remove from this collection all elements in the given collection. If the
	 * collection is an instance of this class, it uses faster iterators.
	 *
	 * @param c
	 *            a collection.
	 * @return <code>true</code> if this collection changed as a result of the
	 *         call.
	 */

	public boolean removeAll(Collection<?> c) {
		boolean retVal = false;
		int n = c.size();

		final Iterator<?> i = c.iterator();
		while (n-- != 0)
			if (remove(i.next()))
				retVal = true;

		return retVal;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public String toString() {
		final StringBuilder s = new StringBuilder();
		final BooleanIterator i = iterator();
		int n = size();
		boolean k;
		boolean first = true;

		s.append("{");

		while (n-- != 0) {
			if (first)
				first = false;
			else
				s.append(", ");
			k = i.nextBoolean();

			s.append(String.valueOf(k));
		}

		s.append("}");
		return s.toString();
	}
}
