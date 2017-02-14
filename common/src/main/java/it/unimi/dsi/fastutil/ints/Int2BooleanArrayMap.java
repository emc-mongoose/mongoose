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

package it.unimi.dsi.fastutil.ints;

import java.util.Map;
import java.util.NoSuchElementException;
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import it.unimi.dsi.fastutil.booleans.BooleanCollection;
import it.unimi.dsi.fastutil.booleans.BooleanCollections;
import it.unimi.dsi.fastutil.booleans.BooleanArraySet;
import it.unimi.dsi.fastutil.booleans.BooleanArrays;

/**
 * A simple, brute-force implementation of a map based on two parallel backing
 * arrays.
 *
 * <p>
 * The main purpose of this implementation is that of wrapping cleanly the
 * brute-force approach to the storage of a very small number of pairs: just put
 * them into two parallel arrays and scan linearly to find an item.
 */

public class Int2BooleanArrayMap extends AbstractInt2BooleanMap
	implements
	java.io.Serializable,
	Cloneable {
	
	private static final long serialVersionUID = 1L;
	/** The keys (valid up to {@link #size}, excluded). */
	private transient int[] key;
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
	public Int2BooleanArrayMap(final int[] key, final boolean[] value) {
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
	public Int2BooleanArrayMap() {
		this.key = IntArrays.EMPTY_ARRAY;
		this.value = BooleanArrays.EMPTY_ARRAY;
	}
	
	/**
	 * Creates a new empty array map of given capacity.
	 *
	 * @param capacity
	 *            the initial capacity.
	 */
	public Int2BooleanArrayMap(final int capacity) {
		this.key = new int[capacity];
		this.value = new boolean[capacity];
	}
	
	/**
	 * Creates a new empty array map copying the entries of a given map.
	 *
	 * @param m
	 *            a map.
	 */
	public Int2BooleanArrayMap(final Int2BooleanMap m) {
		this(m.size());
		putAll(m);
	}
	
	/**
	 * Creates a new empty array map copying the entries of a given map.
	 *
	 * @param m
	 *            a map.
	 */
	public Int2BooleanArrayMap(final Map<? extends Integer, ? extends Boolean> m) {
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
	public Int2BooleanArrayMap(final int[] key, final boolean[] value,
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
		AbstractObjectSet<Int2BooleanMap.Entry> implements Int2BooleanMap.FastEntrySet {
		
		@Override
		public ObjectIterator<Int2BooleanMap.Entry> iterator() {
			return new AbstractObjectIterator<Int2BooleanMap.Entry>() {
				int curr = -1, next = 0;
				
				public boolean hasNext() {
					return next < size;
				}
				
				public Int2BooleanMap.Entry next() {
					if (!hasNext())
						throw new NoSuchElementException();
					return new AbstractInt2BooleanMap.BasicEntry(
						key[curr = next], value[next++]);
				}
				
				public void remove() {
					if (curr == -1)
						throw new IllegalStateException();
					curr = -1;
					final int tail = size-- - next--;
					System.arraycopy(key, next + 1, key, next, tail);
					System.arraycopy(value, next + 1, value, next, tail);
					
				}
			};
		}
		
		public ObjectIterator<Int2BooleanMap.Entry> fastIterator() {
			return new AbstractObjectIterator<Int2BooleanMap.Entry>() {
				int next = 0, curr = -1;
				final BasicEntry entry = new BasicEntry((0), (false));
				
				public boolean hasNext() {
					return next < size;
				}
				
				public Int2BooleanMap.Entry next() {
					if (!hasNext())
						throw new NoSuchElementException();
					entry.key = key[curr = next];
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
					
				}
			};
		}
		
		public int size() {
			return size;
		}
		
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			
			if (e.getKey() == null || !(e.getKey() instanceof Integer))
				return false;
			
			if (e.getValue() == null || !(e.getValue() instanceof Boolean))
				return false;
			
			final int k = ((((Integer) (e.getKey())).intValue()));
			return Int2BooleanArrayMap.this.containsKey(k)
				&& ((Int2BooleanArrayMap.this.get(k)) == (((((Boolean) (e
				.getValue())).booleanValue()))));
		}
		
		@Override
		public boolean remove(final Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			
			if (e.getKey() == null || !(e.getKey() instanceof Integer))
				return false;
			
			if (e.getValue() == null || !(e.getValue() instanceof Boolean))
				return false;
			
			final int k = ((((Integer) (e.getKey())).intValue()));
			final boolean v = ((((Boolean) (e.getValue())).booleanValue()));
			
			final int oldPos = Int2BooleanArrayMap.this.findKey(k);
			if (oldPos == -1
				|| !((v) == (Int2BooleanArrayMap.this.value[oldPos])))
				return false;
			final int tail = size - oldPos - 1;
			System.arraycopy(Int2BooleanArrayMap.this.key, oldPos + 1,
				Int2BooleanArrayMap.this.key, oldPos, tail);
			System.arraycopy(Int2BooleanArrayMap.this.value, oldPos + 1,
				Int2BooleanArrayMap.this.value, oldPos, tail);
			Int2BooleanArrayMap.this.size--;
			
			return true;
		}
	}
	
	public Int2BooleanMap.FastEntrySet int2BooleanEntrySet() {
		return new EntrySet();
	}
	
	private int findKey(final int k) {
		final int[] key = this.key;
		for (int i = size; i-- != 0;)
			if (((key[i]) == (k)))
				return i;
		return -1;
	}
	
	public boolean get(final int k) {
		
		final int[] key = this.key;
		for (int i = size; i-- != 0;)
			if (((key[i]) == (k)))
				return value[i];
		return defRetValue;
	}
	
	public int size() {
		return size;
	}
	
	@Override
	public void clear() {
		size = 0;
	}
	
	@Override
	public boolean containsKey(final int k) {
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
	public boolean put(int k, boolean v) {
		final int oldKey = findKey(k);
		if (oldKey != -1) {
			final boolean oldValue = value[oldKey];
			value[oldKey] = v;
			return oldValue;
		}
		if (size == key.length) {
			final int[] newKey = new int[size == 0 ? 2 : size * 2];
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
	public boolean remove(final int k) {
		
		final int oldPos = findKey(k);
		if (oldPos == -1)
			return defRetValue;
		final boolean oldValue = value[oldPos];
		final int tail = size - oldPos - 1;
		System.arraycopy(key, oldPos + 1, key, oldPos, tail);
		System.arraycopy(value, oldPos + 1, value, oldPos, tail);
		size--;
		
		return oldValue;
	}
	
	@Override
	public IntSet keySet() {
		return new IntArraySet(key, size);
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
	
	public Int2BooleanArrayMap clone() {
		Int2BooleanArrayMap c;
		try {
			c = (Int2BooleanArrayMap) super.clone();
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
			s.writeInt(key[i]);
			s.writeBoolean(value[i]);
		}
	}
	
	private void readObject(java.io.ObjectInputStream s)
	throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		key = new int[size];
		value = new boolean[size];
		for (int i = 0; i < size; i++) {
			key[i] = s.readInt();
			value[i] = s.readBoolean();
		}
	}
}
