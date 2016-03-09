package com.emc.mongoose.common.generator;

import java.text.ParseException;

import static com.emc.mongoose.common.generator.AsyncStringGeneratorFactory.generatorFactory;

public class FormattingGeneratorSkeleton {

	public static final char PATTERN_SYMBOL = '%';
	public static final char[] RANGE_SYMBOLS = {'[',']'};
	public static final char[] FORMAT_SYMBOLS = {'{', '}'};
	public static final char RANGE_DELIMITER = '-';

	private String[] segments;
	private ValueGenerator<String>[] generators;

	@SuppressWarnings("unchecked") // AsyncStringGeneratorFactory always returns ValueGenerator<String> values for generators[]
	public FormattingGeneratorSkeleton(final String pattern) {
		final int patternSymbolsNum = countPatternSymbols(pattern);
		generators = new ValueGenerator[patternSymbolsNum];
		segments = new String[patternSymbolsNum + 1];

	}

	public String[] segments() {
		return segments;
	}


	public static int countPatternSymbols(final String pattern) {
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

	protected void initialize(String pattern, int patternSymbolsNum)
			throws ParseException {
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

	/**
	 *
	 * @param expression expression is a string which follows PATTERN_SYMBOL.
	 * @param binarySymbols - symbols for specifying some parameter between two symbols
	 * @return presence of the parameter. (e.g a range or a format)
	 */
	private boolean isParameterPresented(final StringBuilder expression, final char[] binarySymbols) {
		return expression.length() >= 2 && expression.charAt(1) == binarySymbols[0];
	}

	/**
	 *
	 * @param expression expression is a string which follows PATTERN_SYMBOL.
	 * @param binarySymbols - symbols for specifying some parameter between two symbols
	 * @return a parameter that was extracted from the expression
	 */
	private String getParameter(final StringBuilder expression, final char[] binarySymbols) {
		final int closingSymbolPos = expression.indexOf(String.valueOf(binarySymbols[1]));
		final String parameter = expression.substring(2, closingSymbolPos);
		expression.delete(1, closingSymbolPos + 1);
		return parameter;
	}

	/**
	 *
	 * @param expression expression is a string which follows PATTERN_SYMBOL.
	 * @param binarySymbols - symbols for specifying some parameter between two symbols
	 * @return a parameter that was extracted from the expression or null if there is no parameters
	 */
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
		generators[index] = generatorFactory().createGenerator(type, format, range);
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		result.append("Generators: ");
		for (final ValueGenerator generator : generators) {
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

	protected String format(StringBuilder result) {
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
}
