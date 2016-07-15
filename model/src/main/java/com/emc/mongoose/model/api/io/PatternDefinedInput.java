package com.emc.mongoose.model.api.io;

/**
 Created by kurila on 11.03.16.
 */
public interface PatternDefinedInput
extends Input<String> {
	/**
	 * Special characters
	 */
	char PATTERN_CHAR = '%';
	char[] FORMAT_CHARS = {'{', '}'};

	String getPattern();

	String format(final StringBuilder result);
}
