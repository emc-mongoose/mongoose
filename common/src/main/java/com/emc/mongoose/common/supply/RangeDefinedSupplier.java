package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.supply.BatchSupplier;

/**
 Created by kurila on 11.03.16.
 */
public interface RangeDefinedSupplier<T>
extends BatchSupplier<String> {

	long SHARED_SEED = System.nanoTime() ^ System.currentTimeMillis();
	
	/**
	 * Special characters
	 */
	char[] RANGE_SYMBOLS = {'[',']'};
	char RANGE_DELIMITER = '-';

	T value();
}
