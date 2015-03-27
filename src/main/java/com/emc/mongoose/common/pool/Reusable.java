package com.emc.mongoose.common.pool;
/**
 Created by kurila on 22.12.14.
 */
public interface Reusable
extends Comparable<Reusable> {
	//
	Reusable reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException, InterruptedException;
	//
	void release();
}
