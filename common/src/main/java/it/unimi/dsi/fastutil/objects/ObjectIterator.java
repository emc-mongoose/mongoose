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

import java.util.Iterator;

/**
 * A type-specific {@link Iterator}; provides an additional method to avoid
 * (un)boxing, and the possibility to skip elements.
 *
 * @see Iterator
 */

public interface ObjectIterator<K> extends Iterator<K> {
	/**
	 * Skips the given number of elements.
	 *
	 * <P>
	 * The effect of this call is exactly the same as that of calling
	 * {@link #next()} for <code>n</code> times (possibly stopping if
	 * {@link #hasNext()} becomes false).
	 *
	 * @param n
	 *            the number of elements to skip.
	 * @return the number of elements actually skipped.
	 * @see Iterator#next()
	 */

	int skip(int n);
}
