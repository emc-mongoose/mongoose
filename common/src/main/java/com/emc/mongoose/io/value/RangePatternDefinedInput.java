package com.emc.mongoose.io.value;

import com.emc.mongoose.io.Input;

import java.io.IOException;
public final class RangePatternDefinedInput
extends BasicPatternDefinedInput {

	/**
	 * Special characters
	 */
	public static final char[] RANGE_SYMBOLS = {'[',']'};
	public static final char RANGE_DELIMITER = '-';

	/**
	 * Segments (parts) of the input string that do not require changes
	 */
	private String[] segments;

	public RangePatternDefinedInput(final String pattern)
	throws IllegalArgumentException {
		super(pattern);
	}

	public RangePatternDefinedInput(
		final String pattern,
		final ValueInputFactory<String, ? extends Input<String>> valueInputFactory
	) {
		super(pattern, valueInputFactory);
	}

	private void setSegments(String[] segments) {
		this.segments = segments;
	}

	/**
	 * see the description of the parent class (SimpleFormattingGenerator)
	 */
	@Override
	@SuppressWarnings("unchecked") // AsyncStringGeneratorFactory always returns ValueGenerator<String> values for inputs[]
	protected final void initialize() {
		final int patternSymbolsNum = countPatternSymbols(getPattern());
		if (patternSymbolsNum > 0) {
			setInputs(new Input[patternSymbolsNum]);
			setSegments(new String[patternSymbolsNum + 1]);
			final StringBuilder segmentsBuilder = new StringBuilder();
			final StringBuilder patternBuilder = new StringBuilder(getPattern());
			int segmentCounter = 0;
			for (int j = 0; j < patternSymbolsNum; j++) {
				int i = 0;
				while (patternBuilder.charAt(i) != PATTERN_CHAR) {
					segmentsBuilder.append(patternBuilder.charAt(i)); // building of the segment by character
					i++;
				}
				segments[segmentCounter] = segmentsBuilder.toString();// adding of the segment in 'segments' filed
				segmentsBuilder.setLength(0);
				patternBuilder.delete(0, i + 1); // cutting the segment of the input string
				addExpressionParams(patternBuilder, segmentCounter);
				segmentCounter++;
			}
			segments[patternSymbolsNum] = patternBuilder.toString();
		}
	}

	/**
	 *
	 * @param pattern - input pattern string
	 * @return a number of PATTERN_SYMBOLs in the input pattern string
	 */
	public static int countPatternSymbols(final String pattern) {
		int counter = 0;
		if(!pattern.isEmpty()) {
			final int lastPatternIndex = pattern.length() - 1;
			if(pattern.charAt(lastPatternIndex) == PATTERN_CHAR) {
				throw new IllegalArgumentException();
			}
			final char[] patternChars = pattern.toCharArray();
			for(int i = 0; i < lastPatternIndex; i++) {
				if(patternChars[i] == PATTERN_CHAR) {
					counter++;
					if(patternChars[i + 1] == PATTERN_CHAR) {
						throw new IllegalArgumentException();
					}
				}
			}
		}
		return counter;
	}

	/**
	 * In this method is used to fill the 'inputs' field with value inputs
	 * in accordance with the specified expression parameters
	 * @param expression - a string which follows PATTERN_CHAR
	 * @param index of current empty position in inputs' array ('inputs' field)
	 */
	private void addExpressionParams(final StringBuilder expression, final int index) {
		final char type = expression.charAt(0);
		final String format = initParameter(expression, FORMAT_CHARS);
		final String range = initParameter(expression, RANGE_SYMBOLS);
		expression.delete(0, 1);
		getInputs()[index] = valueInputFactory().createInput(type, format, range);
	}

	/**
	 * This method can be used for debug
	 * @return a string with fields' content
	 */
	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		result.append("Generators: ");
		if (getInputs() != null) {
			for (final Input<String> input : getInputs()) {
				result.append(input.getClass().getName()).append(";");
			}
		}
		result.append("\n");
		result.append("Segments: ");
		if (segments != null) {
			for (final String segment : segments) {
				result.append(segment).append(";");
			}
		}
		result.append("\n");
		return result.toString();
	}

	/**
	 * Assemble output string with 'segments' and 'inputs'
	 * @param result see the description of the parent class (SimpleFormattingGenerator)
	 * @return a string with PATTERN_SYMBOLs replaced by suitable values
	 */
	@Override
	protected final String assembleOutputString(StringBuilder result) {
		try {
			for(int i = 0; i < segments.length - 1; i++) {
				result.append(segments[i]);
				if(getInputs()[i] != null) {
					result.append(getInputs()[i].get());
				}
			}
		} catch(final IOException ignored) {
		}
		result.append(segments[segments.length - 1]);
		return result.toString();
	}
}
