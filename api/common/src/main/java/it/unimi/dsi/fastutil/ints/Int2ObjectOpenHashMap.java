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

package it.unimi.dsi.fastutil.ints;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.AbstractObjectCollection;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import static it.unimi.dsi.fastutil.HashCommon.maxFill;

/**
 * A type-specific hash map with a fast, small-footprint implementation.
 *
 * <P>
 * Instances of this class use a hash table to represent a map. The table is
 * filled up to a specified <em>load factor</em>, and then doubled in size to
 * accommodate new entries. If the table is emptied below <em>one fourth</em> of
 * the load factor, it is halved in size. However, halving is not performed when
 * deleting entries from an iterator, as it would interfere with the iteration
 * process.
 *
 * <p>
 * Note that {@link #clear()} does not modify the hash table size. Rather, a
 * family of {@linkplain #trim() trimming methods} lets you control the size of
 * the table; this is particularly useful if you reuse instances of this class.
 *
 * @see Hash
 * @see HashCommon
 */

public class Int2ObjectOpenHashMap<V> extends AbstractInt2ObjectMap<V>
		implements
			java.io.Serializable,
			Cloneable,
			Hash {

	private static final long serialVersionUID = 0L;
	private static final boolean ASSERTS = false;

	/** The array of keys. */
	protected transient int[] key;

	/** The array of values. */
	protected transient V[] value;

	/** The mask for wrapping a position counter. */
	protected transient int mask;

	/** Whether this set contains the key zero. */
	protected transient boolean containsNullKey;
	/** The current table size. */
	protected transient int n;

	/**
	 * Threshold after which we rehash. It must be the table size times
	 * {@link #f}.
	 */
	protected transient int maxFill;

	/** Number of entries in the set (including the key zero, if present). */
	protected int size;

	/** The acceptable load factor. */
	protected final float f;
	/** Cached set of entries. */
	protected transient FastEntrySet<V> entries;

	/** Cached set of keys. */
	protected transient IntSet keys;

	/** Cached collection of values. */
	protected transient ObjectCollection<V> values;
	/**
	 * Creates a new hash map.
	 *
	 * <p>
	 * The actual table size will be the least power of two greater than
	 * <code>expected</code>/<code>f</code>.
	 *
	 * @param expected
	 *            the expected number of elements in the hash set.
	 * @param f
	 *            the load factor.
	 */
	@SuppressWarnings("unchecked")
	public Int2ObjectOpenHashMap(final int expected, final float f) {

		if (f <= 0 || f > 1)
			throw new IllegalArgumentException(
					"Load factor must be greater than 0 and smaller than or equal to 1");
		if (expected < 0)
			throw new IllegalArgumentException(
					"The expected number of elements must be nonnegative");

		this.f = f;

		n = arraySize(expected, f);
		mask = n - 1;
		maxFill = maxFill(n, f);
		key = new int[n + 1];
		value = (V[]) new Object[n + 1];

	}
	/**
	 * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load
	 * factor.
	 *
	 * @param expected
	 *            the expected number of elements in the hash map.
	 */

	public Int2ObjectOpenHashMap(final int expected) {
		this(expected, DEFAULT_LOAD_FACTOR);
	}
	/**
	 * Creates a new hash map with initial expected
	 * {@link Hash#DEFAULT_INITIAL_SIZE} entries and
	 * {@link Hash#DEFAULT_LOAD_FACTOR} as load factor.
	 */

	public Int2ObjectOpenHashMap() {
		this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
	}
	/**
	 * Creates a new hash map copying a given one.
	 *
	 * @param m
	 *            a {@link Map} to be copied into the new hash map.
	 * @param f
	 *            the load factor.
	 */

	public Int2ObjectOpenHashMap(final Map<? extends Integer, ? extends V> m,
			final float f) {
		this(m.size(), f);
		putAll(m);
	}
	/**
	 * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load
	 * factor copying a given one.
	 *
	 * @param m
	 *            a {@link Map} to be copied into the new hash map.
	 */

	public Int2ObjectOpenHashMap(final Map<? extends Integer, ? extends V> m) {
		this(m, DEFAULT_LOAD_FACTOR);
	}
	/**
	 * Creates a new hash map copying a given type-specific one.
	 *
	 * @param m
	 *            a type-specific map to be copied into the new hash map.
	 * @param f
	 *            the load factor.
	 */

	public Int2ObjectOpenHashMap(final Int2ObjectMap<V> m, final float f) {
		this(m.size(), f);
		putAll(m);
	}
	/**
	 * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load
	 * factor copying a given type-specific one.
	 *
	 * @param m
	 *            a type-specific map to be copied into the new hash map.
	 */

	public Int2ObjectOpenHashMap(final Int2ObjectMap<V> m) {
		this(m, DEFAULT_LOAD_FACTOR);
	}
	/**
	 * Creates a new hash map using the elements of two parallel arrays.
	 *
	 * @param k
	 *            the array of keys of the new hash map.
	 * @param v
	 *            the array of corresponding values in the new hash map.
	 * @param f
	 *            the load factor.
	 * @throws IllegalArgumentException
	 *             if <code>k</code> and <code>v</code> have different lengths.
	 */

	public Int2ObjectOpenHashMap(final int[] k, final V[] v, final float f) {
		this(k.length, f);
		if (k.length != v.length)
			throw new IllegalArgumentException(
					"The key array and the value array have different lengths ("
							+ k.length + " and " + v.length + ")");
		for (int i = 0; i < k.length; i++)
			this.put(k[i], v[i]);
	}
	/**
	 * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load
	 * factor using the elements of two parallel arrays.
	 *
	 * @param k
	 *            the array of keys of the new hash map.
	 * @param v
	 *            the array of corresponding values in the new hash map.
	 * @throws IllegalArgumentException
	 *             if <code>k</code> and <code>v</code> have different lengths.
	 */

	public Int2ObjectOpenHashMap(final int[] k, final V[] v) {
		this(k, v, DEFAULT_LOAD_FACTOR);
	}
	private int realSize() {
		return containsNullKey ? size - 1 : size;
	}

	private void ensureCapacity(final int capacity) {
		final int needed = arraySize(capacity, f);
		if (needed > n)
			rehash(needed);
	}

	private void tryCapacity(final long capacity) {
		final int needed = (int) Math.min(
				1 << 30,
				Math.max(2, HashCommon.nextPowerOfTwo((long) Math.ceil(capacity
						/ f))));
		if (needed > n)
			rehash(needed);
	}

	private V removeEntry(final int pos) {
		final V oldValue = value[pos];

		value[pos] = null;

		size--;

		shiftKeys(pos);
		if (size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE)
			rehash(n / 2);
		return oldValue;
	}

	private V removeNullEntry() {
		containsNullKey = false;

		final V oldValue = value[n];

		value[n] = null;

		size--;

		if (size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE)
			rehash(n / 2);
		return oldValue;
	}

	/** {@inheritDoc} */
	public void putAll(Map<? extends Integer, ? extends V> m) {
		if (f <= .5)
			ensureCapacity(m.size()); // The resulting map will be sized for
										// m.size() elements
		else
			tryCapacity(size() + m.size()); // The resulting map will be
											// tentatively sized for size() +
											// m.size() elements
		super.putAll(m);
	}

	private int insert(final int k, final V v) {
		int pos;

		if (((k) == (0))) {
			if (containsNullKey)
				return n;
			containsNullKey = true;
			pos = n;
		} else {
			int curr;
			final int[] key = this.key;

			// The starting point.
			if (!((curr = key[pos = (HashCommon.mix((k)))
					& mask]) == (0))) {
				if (((curr) == (k)))
					return pos;
				while (!((curr = key[pos = (pos + 1) & mask]) == (0)))
					if (((curr) == (k)))
						return pos;
			}
		}

		key[pos] = k;
		value[pos] = v;
		if (size++ >= maxFill)
			rehash(arraySize(size + 1, f));
		if (ASSERTS)
			checkTable();
		return -1;
	}

	public V put(final int k, final V v) {
		final int pos = insert(k, v);
		if (pos < 0)
			return defRetValue;
		final V oldValue = value[pos];
		value[pos] = v;
		return oldValue;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	@Override
	public V put(final Integer ok, final V ov) {
		final V v = (ov);

		final int pos = insert(((ok).intValue()), v);
		if (pos < 0)
			return (this.defRetValue);
		final V oldValue = value[pos];
		value[pos] = v;
		return (oldValue);
	}
	/**
	 * Shifts left entries with the specified hash code, starting at the
	 * specified position, and empties the resulting free entry.
	 *
	 * @param pos
	 *            a starting position.
	 */
	protected final void shiftKeys(int pos) {
		// Shift entries with the same hash.
		int last, slot;
		int curr;
		final int[] key = this.key;

		for (;;) {
			pos = ((last = pos) + 1) & mask;

			for (;;) {
				if (((curr = key[pos]) == (0))) {
					key[last] = (0);

					value[last] = null;

					return;
				}
				slot = (HashCommon.mix((curr))) & mask;
				if (last <= pos ? last >= slot || slot > pos : last >= slot
						&& slot > pos)
					break;
				pos = (pos + 1) & mask;
			}

			key[last] = curr;
			value[last] = value[pos];

		}
	}

	public V remove(final int k) {
		if (((k) == (0))) {
			if (containsNullKey)
				return removeNullEntry();
			return defRetValue;
		}

		int curr;
		final int[] key = this.key;
		int pos;

		// The starting point.
		if (((curr = key[pos = (HashCommon.mix((k)))
				& mask]) == (0)))
			return defRetValue;
		if (((k) == (curr)))
			return removeEntry(pos);
		while (true) {
			if (((curr = key[pos = (pos + 1) & mask]) == (0)))
				return defRetValue;
			if (((k) == (curr)))
				return removeEntry(pos);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	@Override
	public V remove(final Object ok) {
		final int k = ((((Integer) (ok)).intValue()));
		if (((k) == (0))) {
			if (containsNullKey)
				return (removeNullEntry());
			return (this.defRetValue);
		}

		int curr;
		final int[] key = this.key;
		int pos;

		// The starting point.
		if (((curr = key[pos = (HashCommon.mix((k)))
				& mask]) == (0)))
			return (this.defRetValue);
		if (((curr) == (k)))
			return (removeEntry(pos));
		while (true) {
			if (((curr = key[pos = (pos + 1) & mask]) == (0)))
				return (this.defRetValue);
			if (((curr) == (k)))
				return (removeEntry(pos));
		}
	}
	/** @deprecated Please use the corresponding type-specific method instead. */
	@Deprecated
	public V get(final Integer ok) {
		if (ok == null)
			return null;
		final int k = ((ok).intValue());
		if (((k) == (0)))
			return containsNullKey ? (value[n]) : (this.defRetValue);

		int curr;
		final int[] key = this.key;
		int pos;

		// The starting point.
		if (((curr = key[pos = (HashCommon.mix((k)))
				& mask]) == (0)))
			return (this.defRetValue);
		if (((k) == (curr)))
			return (value[pos]);

		// There's always an unused entry.
		while (true) {
			if (((curr = key[pos = (pos + 1) & mask]) == (0)))
				return (this.defRetValue);
			if (((k) == (curr)))
				return (value[pos]);
		}
	}

	public V get(final int k) {
		if (((k) == (0)))
			return containsNullKey ? value[n] : defRetValue;

		int curr;
		final int[] key = this.key;
		int pos;

		// The starting point.
		if (((curr = key[pos = (HashCommon.mix((k)))
				& mask]) == (0)))
			return defRetValue;
		if (((k) == (curr)))
			return value[pos];
		// There's always an unused entry.
		while (true) {
			if (((curr = key[pos = (pos + 1) & mask]) == (0)))
				return defRetValue;
			if (((k) == (curr)))
				return value[pos];
		}
	}

	public boolean containsKey(final int k) {
		if (((k) == (0)))
			return containsNullKey;

		int curr;
		final int[] key = this.key;
		int pos;

		// The starting point.
		if (((curr = key[pos = (HashCommon.mix((k)))
				& mask]) == (0)))
			return false;
		if (((k) == (curr)))
			return true;
		// There's always an unused entry.
		while (true) {
			if (((curr = key[pos = (pos + 1) & mask]) == (0)))
				return false;
			if (((k) == (curr)))
				return true;
		}
	}

	public boolean containsValue(final Object v) {
		final V value[] = this.value;
		final int key[] = this.key;
		if (containsNullKey
				&& ((value[n]) == null ? (v) == null : (value[n]).equals(v)))
			return true;
		for (int i = n; i-- != 0;)
			if (!((key[i]) == (0))
					&& ((value[i]) == null ? (v) == null : (value[i]).equals(v)))
				return true;
		return false;
	}

	/*
	 * Removes all elements from this map.
	 *
	 * <P>To increase object reuse, this method does not change the table size.
	 * If you want to reduce the table size, you must use {@link #trim()}.
	 */
	public void clear() {
		if (size == 0)
			return;
		size = 0;
		containsNullKey = false;

		Arrays.fill(key, (0));

		Arrays.fill(value, null);

	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * A no-op for backward compatibility.
	 *
	 * @param growthFactor
	 *            unused.
	 * @deprecated Since <code>fastutil</code> 6.1.0, hash tables are doubled
	 *             when they are too full.
	 */
	@Deprecated
	public void growthFactor(int growthFactor) {
	}

	/**
	 * Gets the growth factor (2).
	 *
	 * @return the growth factor of this set, which is fixed (2).
	 * @see #growthFactor(int)
	 * @deprecated Since <code>fastutil</code> 6.1.0, hash tables are doubled
	 *             when they are too full.
	 */
	@Deprecated
	public int growthFactor() {
		return 16;
	}

	/**
	 * The entry class for a hash map does not record key and value, but rather
	 * the position in the hash table of the corresponding entry. This is
	 * necessary so that calls to {@link Map.Entry#setValue(Object)}
	 * are reflected in the map
	 */

	final class MapEntry
			implements
				Int2ObjectMap.Entry<V>,
				Map.Entry<Integer, V> {
		// The table index this entry refers to, or -1 if this entry has been
		// deleted.
		int index;

		MapEntry(final int index) {
			this.index = index;
		}

		MapEntry() {
		}

		/**
		 * {@inheritDoc}
		 *
		 * @deprecated Please use the corresponding type-specific method
		 *             instead.
		 */
		@Deprecated
		public Integer getKey() {
			return (Integer.valueOf(key[index]));
		}

		public int getIntKey() {
			return key[index];
		}

		public V getValue() {
			return (value[index]);
		}

		public V setValue(final V v) {
			final V oldValue = value[index];
			value[index] = v;
			return oldValue;
		}
		@SuppressWarnings("unchecked")
		public boolean equals(final Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<Integer, V> e = (Map.Entry<Integer, V>) o;

			return ((key[index]) == (((e.getKey()).intValue())))
					&& ((value[index]) == null
							? ((e.getValue())) == null
							: (value[index]).equals((e.getValue())));
		}

		public int hashCode() {
			return (key[index])
					^ ((value[index]) == null ? 0 : (value[index]).hashCode());
		}

		public String toString() {
			return key[index] + "=>" + value[index];
		}
	}
	/** An iterator over a hash map. */

	private class MapIterator {
		/**
		 * The index of the last entry returned, if positive or zero; initially,
		 * {@link #n}. If negative, the last entry returned was that of the key
		 * of index {@code - pos - 1} from the {@link #wrapped} list.
		 */
		int pos = n;
		/**
		 * The index of the last entry that has been returned (more precisely,
		 * the value of {@link #pos} if {@link #pos} is positive, or
		 * {@link Integer#MIN_VALUE} if {@link #pos} is negative). It is -1 if
		 * either we did not return an entry yet, or the last returned entry has
		 * been removed.
		 */
		int last = -1;
		/**
		 * A downward counter measuring how many entries must still be returned.
		 */
		int c = size;
		/**
		 * A boolean telling us whether we should return the entry with the null
		 * key.
		 */
		boolean mustReturnNullKey = Int2ObjectOpenHashMap.this.containsNullKey;
		/**
		 * A lazily allocated list containing keys of entries that have wrapped
		 * around the table because of removals.
		 */
		IntArrayList wrapped;

		public boolean hasNext() {
			return c != 0;
		}

		public int nextEntry() {
			if (!hasNext())
				throw new NoSuchElementException();

			c--;
			if (mustReturnNullKey) {
				mustReturnNullKey = false;
				return last = n;
			}

			final int key[] = Int2ObjectOpenHashMap.this.key;

			for (;;) {
				if (--pos < 0) {
					// We are just enumerating elements from the wrapped list.
					last = Integer.MIN_VALUE;
					final int k = wrapped.getInt(-pos - 1);
					int p = (HashCommon.mix((k))) & mask;
					while (!((k) == (key[p])))
						p = (p + 1) & mask;
					return p;
				}
				if (!((key[pos]) == (0)))
					return last = pos;
			}
		}

		/**
		 * Shifts left entries with the specified hash code, starting at the
		 * specified position, and empties the resulting free entry.
		 *
		 * @param pos
		 *            a starting position.
		 */
		private final void shiftKeys(int pos) {
			// Shift entries with the same hash.
			int last, slot;
			int curr;
			final int[] key = Int2ObjectOpenHashMap.this.key;

			for (;;) {
				pos = ((last = pos) + 1) & mask;

				for (;;) {
					if (((curr = key[pos]) == (0))) {
						key[last] = (0);

						value[last] = null;

						return;
					}
					slot = (HashCommon.mix((curr)))
							& mask;
					if (last <= pos ? last >= slot || slot > pos : last >= slot
							&& slot > pos)
						break;
					pos = (pos + 1) & mask;
				}

				if (pos < last) { // Wrapped entry.
					if (wrapped == null)
						wrapped = new IntArrayList(2);
					wrapped.add(key[pos]);
				}

				key[last] = curr;
				value[last] = value[pos];
			}
		}

		public void remove() {
			if (last == -1)
				throw new IllegalStateException();
			if (last == n) {
				containsNullKey = false;

				value[n] = null;

			} else if (pos >= 0)
				shiftKeys(last);
			else {
				// We're removing wrapped entries.

				Int2ObjectOpenHashMap.this.remove(wrapped.getInt(-pos - 1));

				last = -1; // Note that we must not decrement size
				return;
			}

			size--;
			last = -1; // You can no longer remove this entry.
			if (ASSERTS)
				checkTable();
		}

		public int skip(final int n) {
			int i = n;
			while (i-- != 0 && hasNext())
				nextEntry();
			return n - i - 1;
		}
	}

	private class EntryIterator extends MapIterator
			implements
				ObjectIterator<Int2ObjectMap.Entry<V>> {
		private MapEntry entry;

		public Int2ObjectMap.Entry<V> next() {
			return entry = new MapEntry(nextEntry());
		}

		@Override
		public void remove() {
			super.remove();
			entry.index = -1; // You cannot use a deleted entry.
		}
	}

	private class FastEntryIterator extends MapIterator
			implements
				ObjectIterator<Int2ObjectMap.Entry<V>> {
		private final MapEntry entry = new MapEntry();
		public MapEntry next() {
			entry.index = nextEntry();
			return entry;
		}
	}
	private final class MapEntrySet
			extends
				AbstractObjectSet<Int2ObjectMap.Entry<V>>
			implements
				FastEntrySet<V> {

		public ObjectIterator<Int2ObjectMap.Entry<V>> iterator() {
			return new EntryIterator();
		}

		public ObjectIterator<Int2ObjectMap.Entry<V>> fastIterator() {
			return new FastEntryIterator();
		}

		@SuppressWarnings("unchecked")
		public boolean contains(final Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			if (e.getKey() == null || !(e.getKey() instanceof Integer))
				return false;

			final int k = ((((Integer) (e.getKey())).intValue()));
			final V v = ((V) e.getValue());

			if (((k) == (0)))
				return Int2ObjectOpenHashMap.this.containsNullKey
						&& ((value[n]) == null ? (v) == null : (value[n])
								.equals(v));

			int curr;
			final int[] key = Int2ObjectOpenHashMap.this.key;
			int pos;

			// The starting point.
			if (((curr = key[pos = (HashCommon.mix((k)))
					& mask]) == (0)))
				return false;
			if (((k) == (curr)))
				return ((value[pos]) == null ? (v) == null : (value[pos])
						.equals(v));
			// There's always an unused entry.
			while (true) {
				if (((curr = key[pos = (pos + 1) & mask]) == (0)))
					return false;
				if (((k) == (curr)))
					return ((value[pos]) == null ? (v) == null : (value[pos])
							.equals(v));
			}
		}

		@SuppressWarnings("unchecked")
		public boolean remove(final Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			if (e.getKey() == null || !(e.getKey() instanceof Integer))
				return false;

			final int k = ((((Integer) (e.getKey())).intValue()));
			final V v = ((V) e.getValue());

			if (((k) == (0))) {
				if (containsNullKey
						&& ((value[n]) == null ? (v) == null : (value[n])
								.equals(v))) {
					removeNullEntry();
					return true;
				}
				return false;
			}

			int curr;
			final int[] key = Int2ObjectOpenHashMap.this.key;
			int pos;

			// The starting point.
			if (((curr = key[pos = (HashCommon.mix((k)))
					& mask]) == (0)))
				return false;
			if (((curr) == (k))) {
				if (((value[pos]) == null ? (v) == null : (value[pos])
						.equals(v))) {
					removeEntry(pos);
					return true;
				}
				return false;
			}

			while (true) {
				if (((curr = key[pos = (pos + 1) & mask]) == (0)))
					return false;
				if (((curr) == (k))) {
					if (((value[pos]) == null ? (v) == null : (value[pos])
							.equals(v))) {
						removeEntry(pos);
						return true;
					}
				}
			}
		}

		public int size() {
			return size;
		}

		public void clear() {
			Int2ObjectOpenHashMap.this.clear();
		}
	}

	public FastEntrySet<V> int2ObjectEntrySet() {
		if (entries == null)
			entries = new MapEntrySet();

		return entries;
	}

	/**
	 * An iterator on keys.
	 *
	 * <P>
	 * We simply override the {@link java.util.ListIterator#next()}/
	 * {@link java.util.ListIterator#previous()} methods (and possibly their
	 * type-specific counterparts) so that they return keys instead of entries.
	 */
	private final class KeyIterator extends MapIterator implements IntIterator {

		public KeyIterator() {
			super();
		}
		public int nextInt() {
			return key[nextEntry()];
		}

		public Integer next() {
			return (Integer.valueOf(key[nextEntry()]));
		}

	}
	private final class KeySet extends AbstractIntSet {

		public IntIterator iterator() {
			return new KeyIterator();
		}

		public int size() {
			return size;
		}

		public boolean contains(int k) {
			return containsKey(k);
		}

		public boolean remove(int k) {
			final int oldSize = size;
			Int2ObjectOpenHashMap.this.remove(k);
			return size != oldSize;
		}

		public void clear() {
			Int2ObjectOpenHashMap.this.clear();
		}
	}

	public IntSet keySet() {

		if (keys == null)
			keys = new KeySet();
		return keys;
	}

	/**
	 * An iterator on values.
	 *
	 * <P>
	 * We simply override the {@link java.util.ListIterator#next()}/
	 * {@link java.util.ListIterator#previous()} methods (and possibly their
	 * type-specific counterparts) so that they return values instead of
	 * entries.
	 */
	private final class ValueIterator extends MapIterator
			implements
				ObjectIterator<V> {

		public ValueIterator() {
			super();
		}
		public V next() {
			return value[nextEntry()];
		}

	}

	public ObjectCollection<V> values() {
		if (values == null)
			values = new AbstractObjectCollection<V>() {

				public ObjectIterator<V> iterator() {
					return new ValueIterator();
				}

				public int size() {
					return size;
				}

				public boolean contains(Object v) {
					return containsValue(v);
				}

				public void clear() {
					Int2ObjectOpenHashMap.this.clear();
				}
			};

		return values;
	}

	/**
	 * A no-op for backward compatibility. The kind of tables implemented by
	 * this class never need rehashing.
	 *
	 * <P>
	 * If you need to reduce the table size to fit exactly this set, use
	 * {@link #trim()}.
	 *
	 * @return true.
	 * @see #trim()
	 * @deprecated A no-op.
	 */

	@Deprecated
	public boolean rehash() {
		return true;
	}

	/**
	 * Rehashes the map, making the table as small as possible.
	 *
	 * <P>
	 * This method rehashes the table to the smallest size satisfying the load
	 * factor. It can be used when the set will not be changed anymore, so to
	 * optimize access speed and size.
	 *
	 * <P>
	 * If the table size is already the minimum possible, this method does
	 * nothing.
	 *
	 * @return true if there was enough memory to trim the map.
	 * @see #trim(int)
	 */

	public boolean trim() {
		final int l = arraySize(size, f);
		if (l >= n || size > maxFill(l, f))
			return true;
		try {
			rehash(l);
		} catch (OutOfMemoryError cantDoIt) {
			return false;
		}
		return true;
	}

	/**
	 * Rehashes this map if the table is too large.
	 *
	 * <P>
	 * Let <var>N</var> be the smallest table size that can hold
	 * <code>max(n,{@link #size()})</code> entries, still satisfying the load
	 * factor. If the current table size is smaller than or equal to
	 * <var>N</var>, this method does nothing. Otherwise, it rehashes this map
	 * in a table of size <var>N</var>.
	 *
	 * <P>
	 * This method is useful when reusing maps. {@linkplain #clear() Clearing a
	 * map} leaves the table size untouched. If you are reusing a map many times,
	 * you can call this method with a typical size to avoid keeping around a
	 * very large table just because of a few large transient maps.
	 *
	 * @param n
	 *            the threshold for the trimming.
	 * @return true if there was enough memory to trim the map.
	 * @see #trim()
	 */

	public boolean trim(final int n) {
		final int l = HashCommon.nextPowerOfTwo((int) Math.ceil(n / f));
		if (l >= n || size > maxFill(l, f))
			return true;
		try {
			rehash(l);
		} catch (OutOfMemoryError cantDoIt) {
			return false;
		}
		return true;
	}

	/**
	 * Rehashes the map.
	 *
	 * <P>
	 * This method implements the basic rehashing strategy, and may be overriden
	 * by subclasses implementing different rehashing strategies (e.g.,
	 * disk-based rehashing). However, you should not override this method
	 * unless you understand the internal workings of this class.
	 *
	 * @param newN
	 *            the new size
	 */

	@SuppressWarnings("unchecked")
	protected void rehash(final int newN) {
		final int key[] = this.key;
		final V value[] = this.value;

		final int mask = newN - 1; // Note that this is used by the hashing
									// macro
		final int newKey[] = new int[newN + 1];
		final V newValue[] = (V[]) new Object[newN + 1];
		int i = n, pos;

		for (int j = realSize(); j-- != 0;) {
			while (((key[--i]) == (0)));

			if (!((newKey[pos = (HashCommon.mix((key[i])))
					& mask]) == (0)))
				while (!((newKey[pos = (pos + 1) & mask]) == (0)));

			newKey[pos] = key[i];
			newValue[pos] = value[i];
		}

		newValue[newN] = value[n];

		n = newN;
		this.mask = mask;
		maxFill = maxFill(n, f);
		this.key = newKey;
		this.value = newValue;
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
	public Int2ObjectOpenHashMap<V> clone() {
		Int2ObjectOpenHashMap<V> c;
		try {
			c = (Int2ObjectOpenHashMap<V>) super.clone();
		} catch (CloneNotSupportedException cantHappen) {
			throw new InternalError();
		}

		c.keys = null;
		c.values = null;
		c.entries = null;
		c.containsNullKey = containsNullKey;

		c.key = key.clone();
		c.value = value.clone();

		return c;
	}

	/**
	 * Returns a hash code for this map.
	 *
	 * This method overrides the generic method provided by the superclass.
	 * Since <code>equals()</code> is not overriden, it is important that the
	 * value returned by this method is the same value as the one returned by
	 * the overriden method.
	 *
	 * @return a hash code for this map.
	 */

	public int hashCode() {
		int h = 0;
		for (int j = realSize(), i = 0, t = 0; j-- != 0;) {
			while (((key[i]) == (0)))
				i++;

			t = (key[i]);

			if (this != value[i])

				t ^= ((value[i]) == null ? 0 : (value[i]).hashCode());
			h += t;
			i++;
		}
		// Zero / null keys have hash zero.
		if (containsNullKey)
			h += ((value[n]) == null ? 0 : (value[n]).hashCode());
		return h;
	}

	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException {
		final int key[] = this.key;
		final V value[] = this.value;
		final MapIterator i = new MapIterator();

		s.defaultWriteObject();

		for (int j = size, e; j-- != 0;) {
			e = i.nextEntry();
			s.writeInt(key[e]);
			s.writeObject(value[e]);
		}
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();

		n = arraySize(size, f);
		maxFill = maxFill(n, f);
		mask = n - 1;

		final int key[] = this.key = new int[n + 1];
		final V value[] = this.value = (V[]) new Object[n + 1];

		int k;
		V v;

		for (int i = size, pos; i-- != 0;) {

			k = s.readInt();
			v = (V) s.readObject();

			if (((k) == (0))) {
				pos = n;
				containsNullKey = true;
			} else {
				pos = (HashCommon.mix((k))) & mask;
				while (!((key[pos]) == (0)))
					pos = (pos + 1) & mask;
			}

			key[pos] = k;
			value[pos] = v;
		}
		if (ASSERTS)
			checkTable();
	}
	private void checkTable() {
	}
}
