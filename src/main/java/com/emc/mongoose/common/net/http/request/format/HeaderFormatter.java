package com.emc.mongoose.common.net.http.request.format;

// todo add an empty string at the beginning or in the end of the pattern if it is necessary
public class HeaderFormatter {

	private static final char patternSymbol = '%';
	private static final char[] rangeSymbols = {'[',']'};

	char[] types;
	String[] ranges;
	String[] segments;

	public HeaderFormatter(String pattern) {
		int patternSymbolsNum = countPatternSymbols(pattern);
		types = new char[patternSymbolsNum];
		ranges = new String[patternSymbolsNum];
		segments = new String[patternSymbolsNum + 1];
		StringBuilder segmentsBuilder = new StringBuilder();
		StringBuilder patternBuilder = new StringBuilder(pattern);
		int segmentCounter = 0;
		for (int j = 0; j < patternSymbolsNum; j++) {
			int i = 0;
			while (patternBuilder.charAt(i) != patternSymbol) {
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
			if (each == patternSymbol) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 *
	 * @param expression is a string which follows patternSymbol.
	 * @return presence of the range
	 */
	private boolean isRangePresented(StringBuilder expression) {
		return expression.length() >= 2 && expression.charAt(1) == rangeSymbols[0];
	}

	private void addRange(StringBuilder expression, int index) {
		int closingSymbolPos = expression.indexOf(String.valueOf(rangeSymbols[1]));
		ranges[index] = expression.substring(2, closingSymbolPos);
		expression.delete(0, closingSymbolPos + 1);
	}

	private void addExpressionParams(StringBuilder expression, int index) {
		types[index] = expression.charAt(0);
		if (isRangePresented(expression)) {
			addRange(expression, index);
		} else {
			ranges[index] = null;
			expression.delete(0, 1);
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Types: ");
		for (char type: types) {
			result.append(type).append(";");
		}
		result.append("\n");
		result.append("Ranges: ");
		for (String range: ranges) {
			result.append(range).append(";");
		}
		result.append("\n");
		result.append("Segments: ");
		for (String segment: segments) {
			result.append(segment).append(";");
		}
		result.append("\n");
		return result.toString();
	}

	public void format() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < segments.length - 1; i++) {
			result.append(segments[i]).append("%temp%");
		}
		result.append(segments[segments.length - 1]);
		System.out.println(result);
	}


}
