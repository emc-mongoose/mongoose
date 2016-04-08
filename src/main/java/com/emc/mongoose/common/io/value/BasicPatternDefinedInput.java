package com.emc.mongoose.common.io.value;

import com.emc.mongoose.common.io.Input;
import org.apache.commons.lang.NullArgumentException;

import java.io.IOException;
import java.util.List;
/**
 * This class is used ONLY for input pattern strings containing only one expression with the pattern symbol.
 */
public class BasicPatternDefinedInput
implements PatternDefinedInput {

	private static final ThreadLocal<StringBuilder>
			OUTPUT_BUILDER = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};

	/**
	 * A factory for getting of value inputs (see below)
	 */
	private final ValueInputFactory<String, ? extends Input<String>> valueInputFactory;

	/**
	 * An input string with pattern symbols and expressions that have to be replaced by suitable values
	 */
	private final String pattern;

	/**
	 * Generators of values that should be inserted instead of expressions with special characters (see above)
	 */
	private Input<String>[] inputs;

	public
	BasicPatternDefinedInput(final String pattern)
	throws IllegalArgumentException {
		this(pattern, StringInputFactory.getInstance());
	}

	public
	BasicPatternDefinedInput(
		final String pattern,
		final ValueInputFactory<String, ? extends Input<String>> valueInputFactory
	) throws IllegalArgumentException {
		if (pattern == null) {
			throw new NullArgumentException("pattern");
		}
		this.valueInputFactory = valueInputFactory;
		this.pattern = pattern;
		initialize();
	}

	protected final
	ValueInputFactory<String, ? extends Input<String>> valueInputFactory() {
		return valueInputFactory;
	}

	@Override
	public final String getPattern() {
		return pattern;
	}

	protected final Input<String>[] getInputs() {
		return inputs;
	}

	protected final void setInputs(final Input<String>[] inputs) {
		this.inputs = inputs;
	}

	/**
	 * In this method the class fields are being filled
	 */
	@SuppressWarnings("unchecked") // AsyncStringGeneratorFactory always returns ValueGenerator<String> values for getInputs[]
	protected void initialize()
	throws IllegalArgumentException {
		if(pattern.charAt(0) != PATTERN_SYMBOL) {
			throw new IllegalArgumentException();
		}
		final StringBuilder patternBuilder = new StringBuilder(pattern);
		patternBuilder.delete(0, 1);
		final char type = patternBuilder.charAt(0);
		final String format = initParameter(patternBuilder, FORMAT_SYMBOLS);
		setInputs(new Input[] { valueInputFactory.createInput(type, format, null) } );
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
	 * Assemble output string with 'inputs'
	 * @param result see below (format() method)
	 * @return a string with PATTERN_SYMBOLs replaced by suitable values
	 */
	protected String assembleOutputString(final StringBuilder result) {
		try {
			result.append(inputs[0].get());
		} catch(final IOException ignored) {
		}
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
		if(inputs == null) {
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
	//
	@Override
	public final int get(final List<String> buffer, final int limit)
	throws IOException {
		int count = 0;
		final StringBuilder result = OUTPUT_BUILDER.get();
		if(inputs == null) {
			for(; count < limit; count++) {
				result.setLength(0);
				buffer.add(pattern);
			}
		} else {
			for(; count < limit; count++) {
				result.setLength(0);
				buffer.add(assembleOutputString(result));
			}
		}
		return count;
	}
	//
	@Override
	public final void skip(final long count) {
		if(inputs != null) {
			try {
				for(int i = 0; i < inputs.length; i++) {
					inputs[i].skip(count);
				}
			} catch(final IOException ignored) {
			}
		}
	}
	//
	@Override
	public final void reset() {
		if(inputs != null) {
			try {
				for(int i = 0; i < inputs.length; i ++) {
					inputs[i].reset();
				}
			} catch(final IOException ignored) {
			}
		}
	}
	//
	@Override
	public final void close() {
		if(inputs != null) {
			try {
				for(int i = 0; i < inputs.length; i ++) {
					inputs[i].close();
					inputs[i] = null;
				}
			} catch(final IOException ignored) {
			}
			inputs = null;
		}
	}
}
