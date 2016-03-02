package com.emc.mongoose.common.generator;

import java.text.ParseException;

public final class AsyncFormattingGenerator
extends AsyncValueGenerator<String>
implements ValueGenerator<String> {

	public static final char PATTERN_SYMBOL = '%';
	public static final char[] RANGE_SYMBOLS = {'[',']'};
	public static final char[] FORMAT_SYMBOLS = {'{', '}'};
	public static final char RANGE_DELIMITER = '-';

	private final String[] segments;
	private final ValueGenerator<String>[] generators;

	public AsyncFormattingGenerator(final String pattern)
	throws ParseException {
		this(pattern, countPatternSymbols(pattern));
	}

	@SuppressWarnings("unchecked")
	private AsyncFormattingGenerator(final String pattern, final int patternSymbolsNum)
	throws ParseException {
		this(pattern, new String[patternSymbolsNum + 1], new ValueGenerator[patternSymbolsNum]);
	}

	private AsyncFormattingGenerator(
		final String pattern, final String[] segments, final ValueGenerator<String>[] generators
	) throws ParseException {
		super(
			null,
			new InitCallable<String>() {
				private final StringBuilder result = new StringBuilder();
				@Override
				public String call()
				throws Exception {
					result.setLength(0);
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
				public boolean isInitialized() {
					return segments[segments.length - 1] != null;
				}
			}
		);
		this.generators = generators;
		this.segments = segments;
		StringBuilder segmentsBuilder = new StringBuilder();
		StringBuilder patternBuilder = new StringBuilder(pattern);
		final int patternSymbolsNum = segments.length - 1;
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

	private static int countPatternSymbols(final String pattern) {
		int counter = 0;
		int lastPatternIndex = pattern.length() - 1;
		if (pattern.charAt(lastPatternIndex) == PATTERN_SYMBOL) {
			throw new IllegalArgumentException();
		}
		char[] patternChars = pattern.toCharArray();
		for (int i = 0; i < lastPatternIndex; i++) {
			if (patternChars[i] == PATTERN_SYMBOL) {
				counter++;
				if (patternChars[i + 1] == PATTERN_SYMBOL) {
					throw new IllegalArgumentException();
				}
			}
		}
		return counter;
	}

	/**
	 *
	 * @param expression is a string which follows PATTERN_SYMBOL.
	 * @return presence of the range or format
	 */
	private boolean isParameterPresented(final StringBuilder expression, final char[] binarySymbols) {
		return expression.length() >= 2 && expression.charAt(1) == binarySymbols[0];
	}

	private String getParameter(final StringBuilder expression, final char[] binarySymbols) {
		final int closingSymbolPos = expression.indexOf(String.valueOf(binarySymbols[1]));
		String parameter = expression.substring(2, closingSymbolPos);
		expression.delete(1, closingSymbolPos + 1);
		return parameter;
	}

	private String initParameter(final StringBuilder expression, final char[] binarySymbols) {
		if (isParameterPresented(expression, binarySymbols)) {
			return getParameter(expression, binarySymbols);
		}
		return null;
	}

	private void addExpressionParams(final StringBuilder expression, final int index)
	throws ParseException {
		final char type = expression.charAt(0);
		String format = initParameter(expression, FORMAT_SYMBOLS);
		String range = initParameter(expression, RANGE_SYMBOLS);
		expression.delete(0, 1);
		generators[index] = AsyncRangeGeneratorFactory.createGenerator(type, format, range);
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
}
