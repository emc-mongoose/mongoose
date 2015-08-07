package com.emc.mongoose.common.collections;
/**
 Created by kurila on 22.12.14.
 */
public interface Reusable<T> {
	//
	T reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException;
	//
	void release();
}
