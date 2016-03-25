package com.emc.mongoose.common.generator;
/**
 Created by kurila on 11.03.16.
 */
public interface FormatGenerator
extends ValueGenerator<String> {
	/**
	 * Special characters
	 */
	char PATTERN_SYMBOL = '%';
	char[] FORMAT_SYMBOLS = {'{', '}'};

	String getPattern();

	String format(final StringBuilder result);
}
