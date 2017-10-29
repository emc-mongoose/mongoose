package com.emc.mongoose.api.common.supply;

/**
 Created by kurila on 11.03.16.
 */
public interface PatternDefinedSupplier
extends BatchSupplier<String> {

	/**
	 * Special characters
	 */
	char PATTERN_CHAR = '%';
	char FORMAT_BRACKETS[] = {'{', '}'};

	String getPattern();

	String format(final StringBuilder result);
}
