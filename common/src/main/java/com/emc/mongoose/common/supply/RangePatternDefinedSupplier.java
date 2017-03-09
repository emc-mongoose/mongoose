package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.exception.UserShootHisFootException;

import static com.emc.mongoose.common.supply.RangeDefinedSupplier.RANGE_SYMBOLS;

public final class RangePatternDefinedSupplier
extends BasicPatternDefinedSupplier {

	/**
	 * Segments (parts) of the input string that do not require changes
	 */
	private String[] segments;

	public RangePatternDefinedSupplier(final String pattern)
	throws UserShootHisFootException {
		super(pattern);
	}

	public RangePatternDefinedSupplier(
		final String pattern,
		final SupplierFactory<String, ? extends BatchSupplier<String>> supplierFactory
	) throws UserShootHisFootException {
		super(pattern, supplierFactory);
	}

	private void setSegments(String[] segments) {
		this.segments = segments;
	}
	
	private static final ThreadLocal<StringBuilder>
		THREAD_SB_0 = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		},
		THREAD_SB_1 = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};
	/**
	 * see the description of the parent class (SimpleFormattingGenerator)
	 */
	@Override
	protected final void initialize()
	throws UserShootHisFootException {
		final int patternSymbolsNum = countPatternSymbols(getPattern());
		if(patternSymbolsNum > 0) {
			setSuppliers(new BatchSupplier[patternSymbolsNum]);
			setSegments(new String[patternSymbolsNum + 1]);
			final StringBuilder segmentsBuilder = THREAD_SB_0.get();
			segmentsBuilder.setLength(0);
			final StringBuilder patternBuilder = THREAD_SB_1.get();
			patternBuilder.setLength(0);
			patternBuilder.append(getPattern());
			int segmentCounter = 0;
			for(int j = 0; j < patternSymbolsNum; j ++) {
				int i = 0;
				while(patternBuilder.charAt(i) != PATTERN_CHAR) {
					segmentsBuilder.append(patternBuilder.charAt(i)); // building of the segment by character
					i ++;
				}
				segments[segmentCounter] = segmentsBuilder.toString();// adding of the segment in 'segments' filed
				segmentsBuilder.setLength(0);
				patternBuilder.delete(0, i + 1); // cutting the segment of the input string
				addExpressionParams(patternBuilder, segmentCounter);
				segmentCounter ++;
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
	private void addExpressionParams(final StringBuilder expression, final int index)
	throws UserShootHisFootException {
		final char type = expression.charAt(0);
		final String format = initParameter(expression, FORMAT_CHARS);
		final String range = initParameter(expression, RANGE_SYMBOLS);
		expression.delete(0, 1);
		getSuppliers()[index] = getSupplierFactory().createSupplier(type, format, range);
	}

	private static final ThreadLocal<StringBuilder>
		STRING_BULDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};
	/**
	 * This method can be used for debug
	 * @return a string with fields' content
	 */
	@Override
	public String toString() {
		final StringBuilder result = STRING_BULDER.get();
		result.setLength(0); // clean
		result.append("Generators: ");
		if(getSuppliers() != null) {
			for(final BatchSupplier<String> input : getSuppliers()) {
				result.append(input.getClass().getName()).append(";");
			}
		}
		result.append("\n");
		result.append("Segments: ");
		if(segments != null) {
			for(final String segment : segments) {
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
	protected final String assembleOutputString(final StringBuilder result) {
		for(int i = 0; i < segments.length - 1; i ++) {
			result.append(segments[i]);
			if(getSuppliers()[i] != null) {
				result.append(getSuppliers()[i].get());
			}
		}
		result.append(segments[segments.length - 1]);
		return result.toString();
	}
}
