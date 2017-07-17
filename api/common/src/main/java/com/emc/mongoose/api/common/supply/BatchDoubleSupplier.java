package com.emc.mongoose.api.common.supply;

import java.io.Closeable;
import java.util.function.DoubleSupplier;

/**
 Created by kurila on 07.03.17.
 */
public interface BatchDoubleSupplier
extends DoubleSupplier, Closeable {
	
	int get(final double buffer[], final int limit);
	
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
