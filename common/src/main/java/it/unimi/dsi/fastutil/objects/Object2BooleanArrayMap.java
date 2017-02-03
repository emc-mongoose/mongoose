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
/* Object/Reference-only definitions (values) */
/* Primitive-type-only definitions (values) */
/*		 
 * Copyright (C) 2007-2016 Sebastiano Vigna
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

package it.unimi.dsi.fastutil.objects;

import it.unimi.dsi.fastutil.booleans.BooleanArraySet;
import it.unimi.dsi.fastutil.booleans.BooleanArrays;
import it.unimi.dsi.fastutil.booleans.BooleanCollection;
import it.unimi.dsi.fastutil.booleans.BooleanCollections;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A simple, brute-force implementation of a map based on two parallel backing
 * arrays.
 * 
 * <p>
 * The main purpose of this implementation is that of wrapping cleanly the
 * brute-force approach to the storage of a very small number of pairs: just put
 * them into two parallel arrays and scan linearly to find an item.
 */

public class Object2BooleanArrayMap<K> extends AbstractObject2BooleanMap<K>
		implements
			java.io.Serializable,
			Cloneable {

	private static final long serialVersionUID = 1L;
	/** The keys (valid up to {@link #size}, excluded). */
	private transient Object[] key;
	/** The values (parallel to {@link #key}). */
	private transient boolean[] value;
	/** The number of valid entries in {@link #key} and {@link #value}. */
	private int size;

	/**
	 * Creates a new empty array map with given key and value backing arrays.
	 * The resulting map will have as many entries as the given arrays.
	 * 
	 * <p>
	 * It is responsibility of the caller that the elements of <code>key</code>
	 * are distinct.
	 * 
	 * @param key
	 *            the key array.
	 * @param value
	 *            the value array (it <em>must</em> have the same length as
	 *            <code>key</code>).
	 */
	public Object2BooleanArrayMap(final Object[] key, final boolean[] value) {
		this.key = key;
		this.value = value;
		size = key.length;
		if (key.length != value.length)
			throw new IllegalArgumentException(
					"Keys and values have different lengths (" + key.length
							+ ", " + value.length + ")");
	}

	/**
	 * Creates a new empty array map.
	 */
	public Object2BooleanArrayMap() {
		this.key = ObjectArrays.EMPTY_ARRAY;
		this.value = BooleanArrays.EMPTY_ARRAY;
	}

	/**
	 * Creates a new empty array map of given capacity.
	 *
	 * @param capacity
	 *            the initial capacity.
	 */
	public Object2BooleanArrayMap(final int capacity) {
		this.key = new Object[capacity];
		this.value = new boolean[capacity];
	}

	/**
	 * Creates a new empty array map copying the entries of a given map.
	 *
	 * @param m
	 *            a map.
	 */
	public Object2BooleanArrayMap(final Object2BooleanMap<K> m) {
		this(m.size());
		putAll(m);
	}

	/**
	 * Creates a new empty array map copying the entries of a given map.
	 *
	 * @param m
	 *            a map.
	 */
	public Object2BooleanArrayMap(final Map<? extends K, ? extends Boolean> m) {
		this(m.size());
		putAll(m);
	}

	/**
	 * Creates a new array map with given key and value backing arrays, using
	 * the given number of elements.
	 * 
	 * <p>
	 * It is responsibility of the caller that the first <code>size</code>
	 * elements of <code>key</code> are distinct.
	 * 
	 * @param key
	 *            the key array.
	 * @param value
	 *            the value array (it <em>must</em> have the same length as
	 *            <code>key</code>).
	 * @param size
	 *            the number of valid elements in <code>key</code> and
	 *            <code>value</code>.
	 */
	public Object2BooleanArrayMap(final Object[] key, final boolean[] value,
			final int size) {
		this.key = key;
		this.value = value;
		this.size = size;
		if (key.length != value.length)
			throw new IllegalArgumentException(
					"Keys and values have different lengths (" + key.length
							+ ", " + value.length + ")");
		if (size > key.length)
			throw new IllegalArgumentException("The provided size (" + size
					+ ") is larger than or equal to the backing-arrays size ("
					+ key.length + ")");
	}

	private final class EntrySet
			extends
				AbstractObjectSet<Object2BooleanMap.Entry<K>>
			implements
				FastEntrySet<K> {

		@Override
		public ObjectIterator<Object2BooleanMap.Entry<K>> iterator() {
			return new AbstractObjectIterator<Object2BooleanMap.Entry<K>>() {
				int curr = -1, next = 0;

				public boolean hasNext() {
					return next < size;
				}

				@SuppressWarnings("unchecked")
				public Entry<K> next() {
					if (!hasNext())
						throw new NoSuchElementException();
					return new AbstractObject2BooleanMap.BasicEntry<K>(
							(K) key[curr = next], value[next++]);
				}

				public void remove() {
					if (curr == -1)
						throw new IllegalStateException();
					curr = -1;
					final int tail = size-- - next--;
					System.arraycopy(key, next + 1, key, next, tail);
					System.arraycopy(value, next + 1, value, next, tail);

					key[size] = null;

				}
			};
		}

		public ObjectIterator<Object2BooleanMap.Entry<K>> fastIterator() {
			return new AbstractObjectIterator<Object2BooleanMap.Entry<K>>() {
				int next = 0, curr = -1;
				final BasicEntry<K> entry = new BasicEntry<K>((null), (false));

				public boolean hasNext() {
					return next < size;
				}

				@SuppressWarnings("unchecked")
				public Entry<K> next() {
					if (!hasNext())
						throw new NoSuchElementException();
					entry.key = (K) key[curr = next];
					entry.value = value[next++];
					return entry;
				}

				public void remove() {
					if (curr == -1)
						throw new IllegalStateException();
					curr = -1;
					final int tail = size-- - next--;
					System.arraycopy(key, next + 1, key, next, tail);
					System.arraycopy(value, next + 1, value, next, tail);

					key[size] = null;

				}
			};
		}

		public int size() {
			return size;
		}

		@SuppressWarnings("unchecked")
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			if (e.getValue() == null || !(e.getValue() instanceof Boolean))
				return false;

			final K k = ((K) e.getKey());
			return Object2BooleanArrayMap.this.containsKey(k)
					&& ((Object2BooleanArrayMap.this.getBoolean(k)) == (((((Boolean) (e
							.getValue())).booleanValue()))));
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(final Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			if (e.getValue() == null || !(e.getValue() instanceof Boolean))
				return false;

			final K k = ((K) e.getKey());
			final boolean v = ((((Boolean) (e.getValue())).booleanValue()));

			final int oldPos = Object2BooleanArrayMap.this.findKey(k);
			if (oldPos == -1
					|| !((v) == (Object2BooleanArrayMap.this.value[oldPos])))
				return false;
			final int tail = size - oldPos - 1;
			System.arraycopy(Object2BooleanArrayMap.this.key, oldPos + 1,
					Object2BooleanArrayMap.this.key, oldPos, tail);
			System.arraycopy(Object2BooleanArrayMap.this.value, oldPos + 1,
					Object2BooleanArrayMap.this.value, oldPos, tail);
			Object2BooleanArrayMap.this.size--;

			Object2BooleanArrayMap.this.key[size] = null;

			return true;
		}
	}

	public FastEntrySet<K> object2BooleanEntrySet() {
		return new EntrySet();
	}

	private int findKey(final Object k) {
		final Object[] key = this.key;
		for (int i = size; i-- != 0;)
			if (((key[i]) == null ? (k) == null : (key[i]).equals(k)))
				return i;
		return -1;
	}

	public boolean getBoolean(final Object k) {

		final Object[] key = this.key;
		for (int i = size; i-- != 0;)
			if (((key[i]) == null ? (k) == null : (key[i]).equals(k)))
				return value[i];
		return defRetValue;
	}

	public int size() {
		return size;
	}

	@Override
	public void clear() {

		for (int i = size; i-- != 0;) {

			key[i] = null;

		}

		size = 0;
	}

	@Override
	public boolean containsKey(final Object k) {
		return findKey(k) != -1;
	}

	@Override
	public boolean containsValue(boolean v) {
		for (int i = size; i-- != 0;)
			if (((value[i]) == (v)))
				return true;
		return false;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean put(K k, boolean v) {
		final int oldKey = findKey(k);
		if (oldKey != -1) {
			final boolean oldValue = value[oldKey];
			value[oldKey] = v;
			return oldValue;
		}
		if (size == key.length) {
			final Object[] newKey = new Object[size == 0 ? 2 : size * 2];
			final boolean[] newValue = new boolean[size == 0 ? 2 : size * 2];
			for (int i = size; i-- != 0;) {
				newKey[i] = key[i];
				newValue[i] = value[i];
			}
			key = newKey;
			value = newValue;
		}
		key[size] = k;
		value[size] = v;
		size++;
		return defRetValue;
	}

	@Override
	public boolean removeBoolean(final Object k) {

		final int oldPos = findKey(k);
		if (oldPos == -1)
			return defRetValue;
		final boolean oldValue = value[oldPos];
		final int tail = size - oldPos - 1;
		System.arraycopy(key, oldPos + 1, key, oldPos, tail);
		System.arraycopy(value, oldPos + 1, value, oldPos, tail);
		size--;

		key[size] = null;

		return oldValue;
	}

	@Override
	public ObjectSet<K> keySet() {
		return new ObjectArraySet<K>(key, size);
	}

	@Override
	public BooleanCollection values() {
		return BooleanCollections
				.unmodifiable(new BooleanArraySet(value, size));
	}

	/**
	 * Returns a deep copy of this map.
	 *
	 * <P>
	 * This method performs a deep copy of this hash map; the data stored in the
	 * map, however, is not cloned. Note that this makes a difference only for
	 * object keys.
	 *
	 * @return a deep copy of this map.
	 */

	@SuppressWarnings("unchecked")
	public Object2BooleanArrayMap<K> clone() {
		Object2BooleanArrayMap<K> c;
		try {
			c = (Object2BooleanArrayMap<K>) super.clone();
		} catch (CloneNotSupportedException cantHappen) {
			throw new InternalError();
		}
		c.key = key.clone();
		c.value = value.clone();
		return c;
	}

	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException {
		s.defaultWriteObject();
		for (int i = 0; i < size; i++) {
			s.writeObject(key[i]);
			s.writeBoolean(value[i]);
		}
	}

	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		key = new Object[size];
		value = new boolean[size];
		for (int i = 0; i < size; i++) {
			key[i] = s.readObject();
			value[i] = s.readBoolean();
		}
	}
}
