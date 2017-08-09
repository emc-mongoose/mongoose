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

/**
 * A type-specific {@link Iterable} that strengthens that specification of
 * {@link Iterable#iterator()}.
 *
 * <p>
 * <strong>Warning</strong>: Java will let you write &ldquo;colon&rdquo;
 * <code>for</code> statements with primitive-type loop variables; however, what
 * is (unfortunately) really happening is that at each iteration an unboxing
 * (and, in the case of <code>fastutil</code> type-specific data structures, a
 * boxing) will be performed. Watch out.
 *
 * @see Iterable
 */

public interface BooleanIterable
	extends Iterable<Boolean> {

	/**
	 * Returns a type-specific iterator.
	 *
	 * Note that this specification strengthens the one given in
	 * {@link Iterable#iterator()}.
	 *
	 * @return a type-specific iterator.
	 */
	BooleanIterator iterator();
}
