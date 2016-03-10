package com.emc.mongoose.common.generator;

public class SimpleFormattingGenerator implements ValueGenerator<String> {

	/**
	 * Special characters
	 */
	public static final char PATTERN_SYMBOL = '%';
	public static final char[] FORMAT_SYMBOLS = {'{', '}'};

	private final GeneratorFactory<String> generatorFactory;
	private final String pattern;
	/**
	 * Generators of values that should be inserted instead of special characters (see above)
	 */
	private ValueGenerator<String>[] generators;

	public SimpleFormattingGenerator(String pattern) {
		this(pattern, StringGeneratorFactory.generatorFactory());
	}

	public SimpleFormattingGenerator(String pattern, GeneratorFactory<String> generatorFactory) {
		this.generatorFactory = generatorFactory;
		this.pattern = pattern;
		initialize(pattern);

	}

	@SuppressWarnings("unchecked") // AsyncStringGeneratorFactory always returns ValueGenerator<String> values for generators[]
	protected void initialize(String pattern) {
		StringBuilder patternBuilder = new StringBuilder(pattern);
		patternBuilder.delete(0, 1);
		final char type = patternBuilder.charAt(0);
		String format = initParameter(patternBuilder, FORMAT_SYMBOLS);
		setGenerators(new ValueGenerator[]{generatorFactory.createGenerator(type, format, null)});
	}

	protected GeneratorFactory<String> generatorFactory() {
		return generatorFactory;
	}

	public final String pattern() {
		return pattern;
	}

	protected final void setGenerators(ValueGenerator<String>[] generators) {
		this.generators = generators;
	}

	protected final ValueGenerator<String>[] generators() {
		return generators;
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
		if (isParameterPresented(expression, binarySymbols)) {
			return getParameter(expression, binarySymbols);
		}
		return null;
	}

	protected String assembleOutputString(StringBuilder result) {
		result.append(generators[0].get());
		return result.toString();
	}

	/**
	 *
	 * @param result - a parameter to create an opportunity of StringBuilder reusing
	 *                  (StringBuilder instance must be new or cleared with setLength(0))
	 * @return string with PATTERN_SYMBOLs replaced by suitable values
	 */
	protected final String format(StringBuilder result) {
		if (generators != null) {
			return assembleOutputString(result);
		} else {
			return pattern();
		}
	}

	/**
	 * This is a default get() implementation for FormattingGenerator
	 * @return string with PATTERN_SYMBOLs replaced by suitable values
	 */
	@Override
	public String get() {
		return format(new StringBuilder());
	}
}
