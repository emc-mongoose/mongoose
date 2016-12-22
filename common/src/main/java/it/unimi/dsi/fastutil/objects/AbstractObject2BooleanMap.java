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

package it.unimi.dsi.fastutil.objects;

import it.unimi.dsi.fastutil.booleans.AbstractBooleanCollection;
import it.unimi.dsi.fastutil.booleans.AbstractBooleanIterator;
import it.unimi.dsi.fastutil.booleans.BooleanCollection;
import it.unimi.dsi.fastutil.booleans.BooleanIterator;

import java.util.Iterator;
import java.util.Map;

/**
 * An abstract class providing basic methods for maps implementing a
 * type-specific interface.
 *
 * <P>
 * Optional operations just throw an {@link UnsupportedOperationException}.
 * Generic versions of accessors delegate to the corresponding type-specific
 * counterparts following the interface rules (they take care of returning
 * <code>null</code> on a missing key).
 *
 * <P>
 * As a further help, this class provides a {@link BasicEntry BasicEntry} inner
 * class that implements a type-specific version of {@link Map.Entry};
 * it is particularly useful for those classes that do not implement their own
 * entries (e.g., most immutable maps).
 */

public abstract class AbstractObject2BooleanMap<K>
		extends
			AbstractObject2BooleanFunction<K>
		implements
			Object2BooleanMap<K>,
			java.io.Serializable {

	private static final long serialVersionUID = -4940583368468432370L;

	protected AbstractObject2BooleanMap() {
	}

	public boolean containsValue(Object ov) {
		if (ov == null)
			return false;
		return containsValue(((((Boolean) (ov)).booleanValue())));
	}

	/** Checks whether the given value is contained in {@link #values()}. */
	public boolean containsValue(boolean v) {
		return values().contains(v);
	}

	/** Checks whether the given value is contained in {@link #keySet()}. */
	public boolean containsKey(Object k) {
		return keySet().contains(k);
	}

	/**
	 * Puts all pairs in the given map. If the map implements the interface of
	 * this map, it uses the faster iterators.
	 *
	 * @param m
	 *            a map.
	 */

	@SuppressWarnings({"unchecked", "deprecation"})
	public void putAll(Map<? extends K, ? extends Boolean> m) {
		int n = m.size();
		final Iterator<? extends Map.Entry<? extends K, ? extends Boolean>> i = m
				.entrySet().iterator();

		if (m instanceof Object2BooleanMap) {
			Entry<? extends K> e;
			while (n-- != 0) {
				e = (Entry<? extends K>) i.next();
				put(e.getKey(), e.getBooleanValue());
			}
		} else {
			Map.Entry<? extends K, ? extends Boolean> e;
			while (n-- != 0) {
				e = i.next();
				put(e.getKey(), e.getValue());
			}
		}
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * This class provides a basic but complete type-specific entry class for
	 * all those maps implementations that do not have entries on their own
	 * (e.g., most immutable maps).
	 *
	 * <P>
	 * This class does not implement
	 * {@link Map.Entry#setValue(Object) setValue()}, as the
	 * modification would not be reflected in the base map.
	 */

	public static class BasicEntry<K> implements Entry<K> {
		protected K key;
		protected boolean value;

		public BasicEntry(final K key, final Boolean value) {
			this.key = (key);
			this.value = ((value).booleanValue());
		}

		public BasicEntry(final K key, final boolean value) {
			this.key = key;
			this.value = value;
		}
		public K getKey() {
			return (key);
		}
		/**
		 * {@inheritDoc}
		 *
		 * @deprecated Please use the corresponding type-specific method
		 *             instead.
		 */
		@Deprecated
		public Boolean getValue() {
			return (Boolean.valueOf(value));
		}

		public boolean getBooleanValue() {
			return value;
		}

		public boolean setValue(final boolean value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 *
		 * @deprecated Please use the corresponding type-specific method
		 *             instead.
		 */
		@Deprecated
		public Boolean setValue(final Boolean value) {
			return Boolean.valueOf(setValue(value.booleanValue()));
		}

		public boolean equals(final Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			if (e.getValue() == null || !(e.getValue() instanceof Boolean))
				return false;

			return ((key) == null ? ((e.getKey())) == null : (key).equals((e
					.getKey())))
					&& ((value) == (((((Boolean) (e.getValue())).booleanValue()))));
		}

		public int hashCode() {
			return ((key) == null ? 0 : (key).hashCode())
					^ (value ? 1231 : 1237);
		}

		public String toString() {
			return key + "->" + value;
		}
	}

	/**
	 * Returns a type-specific-set view of the keys of this map.
	 *
	 * <P>
	 * The view is backed by the set returned by {@link #entrySet()}. Note that
	 * <em>no attempt is made at caching the result of this method</em>, as this
	 * would require adding some attributes that lightweight implementations
	 * would not need. Subclasses may easily override this policy by calling
	 * this method and caching the result, but implementors are encouraged to
	 * write more efficient ad-hoc implementations.
	 *
	 * @return a set view of the keys of this map; it may be safely cast to a
	 *         type-specific interface.
	 */

	public ObjectSet<K> keySet() {
		return new AbstractObjectSet<K>() {

			public boolean contains(final Object k) {
				return containsKey(k);
			}

			public int size() {
				return AbstractObject2BooleanMap.this.size();
			}
			public void clear() {
				AbstractObject2BooleanMap.this.clear();
			}

			public ObjectIterator<K> iterator() {
				return new AbstractObjectIterator<K>() {
					final ObjectIterator<Map.Entry<K, Boolean>> i = entrySet()
							.iterator();
					@Override
					public K next() {
						return ((Entry<K>) i.next()).getKey();
					};
					@Override
					public boolean hasNext() {
						return i.hasNext();
					}
					@Override
					public void remove() {
						i.remove();
					}
				};
			}
		};
	}

	/**
	 * Returns a type-specific-set view of the values of this map.
	 *
	 * <P>
	 * The view is backed by the set returned by {@link #entrySet()}. Note that
	 * <em>no attempt is made at caching the result of this method</em>, as this
	 * would require adding some attributes that lightweight implementations
	 * would not need. Subclasses may easily override this policy by calling
	 * this method and caching the result, but implementors are encouraged to
	 * write more efficient ad-hoc implementations.
	 *
	 * @return a set view of the values of this map; it may be safely cast to a
	 *         type-specific interface.
	 */

	public BooleanCollection values() {
		return new AbstractBooleanCollection() {

			public boolean contains(final boolean k) {
				return containsValue(k);
			}

			public int size() {
				return AbstractObject2BooleanMap.this.size();
			}
			public void clear() {
				AbstractObject2BooleanMap.this.clear();
			}

			public BooleanIterator iterator() {
				return new AbstractBooleanIterator() {
					final ObjectIterator<Map.Entry<K, Boolean>> i = entrySet()
							.iterator();

					/**
					 * {@inheritDoc}
					 *
					 * @deprecated Please use the corresponding type-specific
					 *             method instead.
					 */
					@Deprecated
					public boolean nextBoolean() {
						return ((Entry<K>) i.next())
								.getBooleanValue();
					};

					public boolean hasNext() {
						return i.hasNext();
					}
				};
			}
		};
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public ObjectSet<Map.Entry<K, Boolean>> entrySet() {
		return (ObjectSet) object2BooleanEntrySet();
	}

	/**
	 * Returns a hash code for this map.
	 *
	 * The hash code of a map is computed by summing the hash codes of its
	 * entries.
	 *
	 * @return a hash code for this map.
	 */

	public int hashCode() {
		int h = 0, n = size();
		final ObjectIterator<? extends Map.Entry<K, Boolean>> i = entrySet()
				.iterator();

		while (n-- != 0)
			h += i.next().hashCode();
		return h;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Map))
			return false;

		Map<?, ?> m = (Map<?, ?>) o;
		if (m.size() != size())
			return false;
		return entrySet().containsAll(m.entrySet());
	}

	public String toString() {
		final StringBuilder s = new StringBuilder();
		final ObjectIterator<? extends Map.Entry<K, Boolean>> i = entrySet()
				.iterator();
		int n = size();
		Entry<K> e;
		boolean first = true;

		s.append("{");

		while (n-- != 0) {
			if (first)
				first = false;
			else
				s.append(", ");

			e = (Entry<K>) i.next();

			if (this == e.getKey())
				s.append("(this map)");
			else

				s.append(String.valueOf(e.getKey()));
			s.append("=>");

			s.append(String.valueOf(e.getBooleanValue()));
		}

		s.append("}");
		return s.toString();
	}

}
