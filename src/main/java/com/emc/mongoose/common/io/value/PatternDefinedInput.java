package com.emc.mongoose.common.io.value;
import com.emc.mongoose.common.io.Input;
/**
 Created by kurila on 11.03.16.
 */
public interface PatternDefinedInput
extends Input<String> {
	/**
	 * Special characters
	 */
	char PATTERN_SYMBOL = '%';
	char[] FORMAT_SYMBOLS = {'{', '}'};

	String getPattern();

	String format(final StringBuilder result);
}
