package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.supply.BatchSupplier;

/**
 Created by kurila on 11.03.16.
 */
public interface PatternDefinedSupplier
extends BatchSupplier<String> {

	/**
	 * Special characters
	 */
	char PATTERN_CHAR = '%';
	char FORMAT_CHARS[] = {'{', '}'};

	String getPattern();

	String format(final StringBuilder result);
}
