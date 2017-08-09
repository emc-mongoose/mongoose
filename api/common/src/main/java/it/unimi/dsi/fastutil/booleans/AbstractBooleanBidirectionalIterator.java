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
 * An abstract class facilitating the creation of type-specific
 * {@linkplain it.unimi.dsi.fastutil.BidirectionalIterator bidirectional
 * iterators}.
 *
 * <P>
 * To create a type-specific bidirectional iterator, besides what is needed for
 * an iterator you need both a method returning the previous element as
 * primitive type and a method returning the previous element as an object.
 * However, if you inherit from this class you need just one (anyone).
 *
 * <P>
 * This class implements also a trivial version of {@link #back(int)} that uses
 * type-specific methods.
 */

public abstract class AbstractBooleanBidirectionalIterator
		extends
			AbstractBooleanIterator implements BooleanBidirectionalIterator {

	protected AbstractBooleanBidirectionalIterator() {
	}

	/** Delegates to the corresponding generic method. */
	public boolean previousBoolean() {
		return previous().booleanValue();
	}

	/** Delegates to the corresponding type-specific method. */
	public Boolean previous() {
		return Boolean.valueOf(previousBoolean());
	}

	/**
	 * This method just iterates the type-specific version of
	 * {@link #previous()} for at most <code>n</code> times, stopping if
	 * {@link #hasPrevious()} becomes false.
	 */
	public int back(final int n) {
		int i = n;
		while (i-- != 0 && hasPrevious())
			previousBoolean();
		return n - i - 1;
	}

}
