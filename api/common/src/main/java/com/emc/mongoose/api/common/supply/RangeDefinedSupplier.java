package com.emc.mongoose.api.common.supply;

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
