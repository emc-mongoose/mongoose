package com.emc.mongoose.common.supply;

import java.io.Closeable;
import java.util.function.LongSupplier;

/**
 Created by kurila on 07.03.17.
 */
public interface BatchLongSupplier
extends LongSupplier, Closeable {
	
	int get(final long buffer[], final int limit);
	
	/**
	 * Skip some items.
	 * @param count count of items should be skipped from the input stream
	 */
	long skip(final long count);
	
	/**
	 Reset this input making this readable from the beginning
	 */
	void reset();
}
