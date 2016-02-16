package com.emc.mongoose.common.net.http.request.format;

import com.emc.mongoose.common.generator.ValueGenerator;

public class HeaderFormatter {

	public static final char PATTERN_SYMBOL = '%';
	public static final char[] RANGE_SYMBOLS = {'[',']'};
	public static final char RANGE_DELIMITER = '-';

	private String[] segments;
	private ValueGenerator[] generators;

	public HeaderFormatter(String pattern) {
		int patternSymbolsNum = countPatternSymbols(pattern);
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

	private int countPatternSymbols(String pattern) {
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
	private boolean isRangePresented(StringBuilder expression) {
		return expression.length() >= 2 && expression.charAt(1) == RANGE_SYMBOLS[0];
	}

	private String getRange(StringBuilder expression) {
		int closingSymbolPos = expression.indexOf(String.valueOf(RANGE_SYMBOLS[1]));
		String range = expression.substring(2, closingSymbolPos);
		expression.delete(0, closingSymbolPos + 1);
		return range;
	}

	private void addExpressionParams(StringBuilder expression, int index) {
		char type = expression.charAt(0);
		if (isRangePresented(expression)) {
			generators[index] = HeaderValueGeneratorFactory.createGenerator(type, getRange(expression));
		} else {
			generators[index] = HeaderValueGeneratorFactory.createGenerator(type);
			expression.delete(0, 1);
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Generators: ");
		for (ValueGenerator generator: generators) {
			result.append(generator.getClass().getName()).append(";");
		}
		result.append("\n");
		result.append("Segments: ");
		for (String segment: segments) {
			result.append(segment).append(";");
		}
		result.append("\n");
		return result.toString();
	}

	public String format() {
		StringBuilder result = new StringBuilder();
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
