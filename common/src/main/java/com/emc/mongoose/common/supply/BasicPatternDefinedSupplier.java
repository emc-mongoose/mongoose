package com.emc.mongoose.common.supply;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import static com.emc.mongoose.common.supply.RangeDefinedSupplier.SEED_BRACKETS;

import java.io.IOException;
import java.util.List;

/**
 * This class is used ONLY for input pattern strings containing only one expression with the pattern symbol.
 */
public class BasicPatternDefinedSupplier
implements PatternDefinedSupplier {

	private static final ThreadLocal<StringBuilder>
		OUTPUT_BUILDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};

	// A factory for getting of value suppliers (see below)
	private final SupplierFactory<String, ? extends BatchSupplier<String>> supplierFactory;

	// An input string with pattern symbols and expressions that have to be replaced by suitable values
	private final String pattern;

	/**
	 * Generators of values that should be inserted instead of expressions with special characters (see above)
	 */
	private BatchSupplier<String>[] suppliers;

	public BasicPatternDefinedSupplier(final String pattern)
	throws UserShootHisFootException {
		this(pattern, StringSupplierFactory.getInstance());
	}

	public BasicPatternDefinedSupplier(
		final String pattern,
		final SupplierFactory<String, ? extends BatchSupplier<String>> supplierFactory
	) throws UserShootHisFootException {
		if(pattern == null) {
			throw new UserShootHisFootException("Null pattern");
		}
		this.supplierFactory = supplierFactory;
		this.pattern = pattern;
		initialize();
	}

	protected final SupplierFactory<String, ? extends BatchSupplier<String>> getSupplierFactory() {
		return supplierFactory;
	}

	@Override
	public final String getPattern() {
		return pattern;
	}

	protected final BatchSupplier<String>[] getSuppliers() {
		return suppliers;
	}

	protected final void setSuppliers(final BatchSupplier<String>[] suppliers) {
		this.suppliers = suppliers;
	}
	
	private static final ThreadLocal<StringBuilder>
		STRING_BULDER = new ThreadLocal<StringBuilder>() {
			@Override
			protected final StringBuilder initialValue() {
				return new StringBuilder();
			}
		};
	/**
	 * In this method the class fields are being filled
	 */
	@SuppressWarnings("unchecked") // AsyncStringGeneratorFactory always returns ValueGenerator<String> values for getSuppliers[]
	protected void initialize()
	throws UserShootHisFootException {
		if(pattern.charAt(0) != PATTERN_CHAR) {
			throw new UserShootHisFootException();
		}
		final StringBuilder patternBuilder = STRING_BULDER.get();
		patternBuilder.setLength(0);
		patternBuilder.append(pattern);
		patternBuilder.delete(0, 1);
		final char type = patternBuilder.charAt(0);
		final String formatStr = initParameter(patternBuilder, FORMAT_BRACKETS);
		final String seedStr = initParameter(patternBuilder, SEED_BRACKETS);
		setSuppliers(
			new BatchSupplier[] {
				supplierFactory.createSupplier(type, seedStr, formatStr, null)
			}
		);
	}

	/**
	 *
	 * @param expression - a string which follows some pattern symbol
	 * @param binarySymbols - symbols for specifying some parameter between two symbols
	 * @return presence of the parameter. (e.g a range or a format)
	 */
	protected final boolean isParameterPresented(
		final StringBuilder expression, final char[] binarySymbols
	) {
		return expression.length() >= 2 && expression.charAt(1) == binarySymbols[0];
	}

	/**
	 *
	 * @param expression - a string which follows some pattern symbol
	 * @param binarySymbols - symbols for specifying some parameter between two symbols
	 * @return a parameter that was extracted from the expression
	 */
	protected final String getParameter(
		final StringBuilder expression, final char[] binarySymbols
	) {
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
	protected final String initParameter(
		final StringBuilder expression, final char[] binarySymbols
	) {
		if(isParameterPresented(expression, binarySymbols)) {
			return getParameter(expression, binarySymbols);
		}
		return null;
	}

	/**
	 * Assemble output string with 'suppliers'
	 * @param result see below (format() method)
	 * @return a string with PATTERN_SYMBOLs replaced by suitable values
	 */
	protected String assembleOutputString(final StringBuilder result) {
		return result.append(suppliers[0].get()).toString();
	}

	/**
	 *
	 * @param result - a parameter to create an opportunity of StringBuilder reusing
	 *                  (StringBuilder instance must be cleared with setLength(0))
	 * @return a string with PATTERN_SYMBOLs replaced by suitable values
	 */
	@Override
	public final String format(final StringBuilder result) {
		if(suppliers == null) {
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
	
	@Override
	public final int get(final List<String> buffer, final int limit) {
		int count = 0;
		final StringBuilder result = OUTPUT_BUILDER.get();
		if(suppliers == null) {
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
	
	@Override
	public final long skip(final long count) {
		if(suppliers != null) {
			for(int i = 0; i < suppliers.length; i++) {
				suppliers[i].skip(count);
			}
		}
		return count;
	}
	
	@Override
	public final void reset() {
		if(suppliers != null) {
			for(int i = 0; i < suppliers.length; i ++) {
				suppliers[i].reset();
			}
		}
	}
	
	@Override
	public final void close()
	throws IOException {
		if(suppliers != null) {
			for(int i = 0; i < suppliers.length; i ++) {
				suppliers[i].close();
				suppliers[i] = null;
			}
			suppliers = null;
		}
	}
}
