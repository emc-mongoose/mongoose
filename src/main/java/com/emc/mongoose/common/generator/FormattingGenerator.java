package com.emc.mongoose.common.generator;

import java.text.ParseException;
public final class FormattingGenerator implements ValueGenerator<String> {

	public static final char PATTERN_SYMBOL = '%';
	public static final char[] RANGE_SYMBOLS = {'[',']'};
	public static final char RANGE_DELIMITER = '-';

	private String[] segments;
	private ValueGenerator[] generators;

	public FormattingGenerator(final String pattern)
	throws ParseException {
		final int patternSymbolsNum = countPatternSymbols(pattern);
		generators = new ValueGenerator[patternSymbolsNum];
		segments = new String[patternSymbolsNum + 1];
		StringBuilder segmentsBuilder = new StringBuilder();
		StringBuilder patternBuilder = new StringBuilder(pattern);
		int segmentCounter = 0;
		for (int j = 0; j < patternSymbolsNum; j++) {
			int i = 0;
			while (patternBuilder.charAt(i) != PATTERN_SYMBOL) {
				segmentsBuilder.append(patternBuilder.charAt(i));
				i++;
			}
			segments[segmentCounter] = segmentsBuilder.toString();
			segmentsBuilder.setLength(0);
			patternBuilder.delete(0, i + 1);
			addExpressionParams(patternBuilder, segmentCounter);
			segmentCounter++;
		}
		segments[patternSymbolsNum] = patternBuilder.toString();
	}

	private int countPatternSymbols(final String pattern) {
		int counter = 0;
		for (char each : pattern.toCharArray()) {
			if (each == PATTERN_SYMBOL) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 *
	 * @param expression is a string which follows PATTERN_SYMBOL.
	 * @return presence of the range
	 */
	private boolean isRangePresented(final StringBuilder expression) {
		return expression.length() >= 2 && expression.charAt(1) == RANGE_SYMBOLS[0];
	}

	private String getRange(final StringBuilder expression) {
		final int closingSymbolPos = expression.indexOf(String.valueOf(RANGE_SYMBOLS[1]));
		String range = expression.substring(2, closingSymbolPos);
		expression.delete(0, closingSymbolPos + 1);
		return range;
	}

	private void addExpressionParams(final StringBuilder expression, final int index)
	throws ParseException {
		final char type = expression.charAt(0);
		if (isRangePresented(expression)) {
			generators[index] = AsyncRangeGeneratorFactory.createGenerator(type, getRange(expression));
		} else {
			generators[index] = AsyncRangeGeneratorFactory.createGenerator(type);
			expression.delete(0, 1);
		}
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		result.append("Generators: ");
		for (final ValueGenerator generator: generators) {
			result.append(generator.getClass().getName()).append(";");
		}
		result.append("\n");
		result.append("Segments: ");
		for (final String segment: segments) {
			result.append(segment).append(";");
		}
		result.append("\n");
		return result.toString();
	}

	public String format() {
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < segments.length - 1; i++) {
			result.append(segments[i]);
			if (generators[i] != null) {
				result.append(generators[i].get());
			}
		}
		result.append(segments[segments.length - 1]);
		return result.toString();
	}

	@Override
	public String get() {
		return format();
	}
}
