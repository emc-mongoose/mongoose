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

import java.util.Set;

/**
 * A type-specific {@link Set}; provides some additional methods that use
 * polymorphism to avoid (un)boxing.
 *
 * <P>
 * Additionally, this interface strengthens (again) {@link #iterator()}.
 *
 * @see Set
 */

public interface ObjectSet<K> extends ObjectCollection<K>, Set<K> {

	/**
	 * Returns a type-specific iterator on the elements of this set.
	 *
	 * <p>
	 * Note that this specification strengthens the one given in
	 * {@link Iterable#iterator()}, which was already strengthened in
	 * the corresponding type-specific class, but was weakened by the fact that
	 * this interface extends {@link Set}.
	 *
	 * @return a type-specific iterator on the elements of this set.
	 */
	ObjectIterator<K> iterator();

	/**
	 * Removes an element from this set.
	 *
	 * <p>
	 * Note that the corresponding method of the type-specific collection is
	 * <code>rem()</code>. This unfortunate situation is caused by the clash
	 * with the similarly named index-based method in the {@link java.util.List}
	 * interface.
	 *
	 * @see java.util.Collection#remove(Object)
	 */
	public boolean remove(Object k);
}
