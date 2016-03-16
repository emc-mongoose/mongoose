package com.emc.mongoose.common.generator;

import org.apache.commons.lang.NullArgumentException;

/**
 * This class is used ONLY for input pattern strings containing only one expression with the pattern symbol.
 */
public class BasicFormattingGenerator
implements FormattingGenerator {

	private static final ThreadLocal<StringBuilder>
			OUTPUT_BUILDER = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};

	/**
	 * A factory for getting of value generators (see below)
	 */
	private final GeneratorFactory<String, ValueGenerator<String>> generatorFactory;

	/**
	 * An input string with pattern symbols and expressions that have to be replaced by suitable values
	 */
	private final String pattern;

	/**
	 * Generators of values that should be inserted instead of expressions with special characters (see above)
	 */
	private ValueGenerator<String>[] generators;

	public BasicFormattingGenerator(final String pattern) {
		this(pattern, StringGeneratorFactory.generatorFactory());
	}

	public BasicFormattingGenerator(
		final String pattern, GeneratorFactory<String, ValueGenerator<String>> generatorFactory
	) {
		if (pattern == null) {
			throw new NullArgumentException("pattern");
		}
		this.generatorFactory = generatorFactory;
		this.pattern = pattern;
		initialize();
	}

	protected final GeneratorFactory<String, ValueGenerator<String>> generatorFactory() {
		return generatorFactory;
	}

	@Override
	public final String getPattern() {
		return pattern;
	}

	protected final ValueGenerator<String>[] generators() {
		return generators;
	}

	protected final void setGenerators(final ValueGenerator<String>[] generators) {
		this.generators = generators;
	}

	/**
	 * In this method the class fields are being filled
	 */
	@SuppressWarnings("unchecked") // AsyncStringGeneratorFactory always returns ValueGenerator<String> values for generators[]
	protected void initialize() {
		if(pattern.charAt(0) != PATTERN_SYMBOL) {
			throw new IllegalArgumentException();
		}
		final StringBuilder patternBuilder = new StringBuilder(pattern);
		patternBuilder.delete(0, 1);
		final char type = patternBuilder.charAt(0);
		final String format = initParameter(patternBuilder, FORMAT_SYMBOLS);
		setGenerators(new ValueGenerator[]{generatorFactory.createGenerator(type, format, null)});
	}

	/**
	 *
	 * @param expression - a string which follows some pattern symbol
	 * @param binarySymbols - symbols for specifying some parameter between two symbols
	 * @return presence of the parameter. (e.g a range or a format)
	 */
	protected final boolean isParameterPresented(final StringBuilder expression, final char[] binarySymbols) {
		return expression.length() >= 2 && expression.charAt(1) == binarySymbols[0];
	}

	/**
	 *
	 * @param expression - a string which follows some pattern symbol
	 * @param binarySymbols - symbols for specifying some parameter between two symbols
	 * @return a parameter that was extracted from the expression
	 */
	protected final String getParameter(final StringBuilder expression, final char[] binarySymbols) {
		final int closingSymbolPos = expression.indexOf(String.valueOf(binarySymbols[1]));
		final String parameter = expression.substring(2, closingSymbolPos);
		expression.delete(1, closingSymbolPos + 1);
		return parameter;
	}

	/**
	 *
	 * @param expression - a string which follows some pattern symbol
	 * @param binarySymbols - symbols for specifying some parameter between two symbols
	 * @return a parameter that was extracted from the expression or null if there is no parameters
	 */
	protected final String initParameter(final StringBuilder expression, final char[] binarySymbols) {
		if(isParameterPresented(expression, binarySymbols)) {
			return getParameter(expression, binarySymbols);
		}
		return null;
	}

	/**
	 * Assemble output string with 'generators'
	 * @param result see below (format() method)
	 * @return a string with PATTERN_SYMBOLs replaced by suitable values
	 */
	protected String assembleOutputString(final StringBuilder result) {
		result.append(generators[0].get());
		return result.toString();
	}

	/**
	 *
	 * @param result - a parameter to create an opportunity of StringBuilder reusing
	 *                  (StringBuilder instance must be cleared with setLength(0))
	 * @return a string with PATTERN_SYMBOLs replaced by suitable values
	 */
	@Override
	public final String format(final StringBuilder result) {
		if(generators == null) {
			return getPattern();
		} else {
			return assembleOutputString(result);
		}
	}

	/**
	 * This is a default get() implementation for SimpleFormattingGenerator
	 * @return string with PATTERN_SYMBOLs replaced by suitable values
	 */
	@Override
	public final String get() {
		final StringBuilder result = OUTPUT_BUILDER.get();
		result.setLength(0);
		return format(result);
	}
}
