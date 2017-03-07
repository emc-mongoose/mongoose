package com.emc.mongoose.common.supply;

import java.io.Closeable;
import java.util.List;
import java.util.function.Supplier;

/**
 Created by kurila on 07.03.17.
 */
public interface BatchSupplier<T>
extends Supplier<T>, Closeable {
	
	int get(final List<T> buffer, final int limit);
	
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
