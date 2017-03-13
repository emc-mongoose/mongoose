package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.supply.BatchSupplier;

/**
 Created by kurila on 11.03.16.
 */
public interface RangeDefinedSupplier<T>
extends BatchSupplier<String> {

	/**
	 * Special characters
	 */
	char[] RANGE_BRACKETS = {'[',']'};
	char RANGE_DELIMITER = '-';

	char[] SEED_BRACKETS = {'(', ')'};

	T value();
}
