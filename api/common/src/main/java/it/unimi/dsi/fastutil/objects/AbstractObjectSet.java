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
 * An abstract class providing basic methods for sets implementing a
 * type-specific interface.
 */

public abstract class AbstractObjectSet<K> extends AbstractObjectCollection<K>
		implements
			Cloneable,
			ObjectSet<K> {

	protected AbstractObjectSet() {
	}

	public abstract ObjectIterator<K> iterator();

	public boolean equals(final Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Set))
			return false;

		Set<?> s = (Set<?>) o;
		if (s.size() != size())
			return false;
		return containsAll(s);
	}

	/**
	 * Returns a hash code for this set.
	 *
	 * The hash code of a set is computed by summing the hash codes of its
	 * elements.
	 *
	 * @return a hash code for this set.
	 */

	public int hashCode() {
		int h = 0, n = size();
		ObjectIterator<K> i = iterator();
		K k;

		while (n-- != 0) {
			k = i.next(); // We need k because KEY2JAVAHASH() is a macro with
							// repeated evaluation.
			h += ((k) == null ? 0 : (k).hashCode());
		}
		return h;
	}
}
