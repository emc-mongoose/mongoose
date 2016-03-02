package com.emc.mongoose.common.generator;

import java.text.DecimalFormat;
import java.text.Format;

public final class AsyncDoubleGenerator
extends AsyncFormatRangeGeneratorBase<Double> {
	;
	public AsyncDoubleGenerator(final Double minValue, final Double maxValue, final String formatString) {
		super(minValue, maxValue, formatString);
	}

	public AsyncDoubleGenerator(final Double initialValue, final String formatString) {
		super(initialValue, formatString);
	}

	/**
	 *
	 * @param formatString - a pattern for DecimalFormat. It should match a pattern,
	 *                        that described here
	 *                        https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html
	 *                        (see Special Pattern Characters paragraph of examples)
	 * @return a suitable formatter for numbers of a double type
	 */
	@Override
	Format getFormatterInstance(String formatString) {
		return new DecimalFormat(formatString);
	}

	@Override
	protected final Double computeRange(final Double minValue, final Double maxValue) {
		return maxValue - minValue + 1.0;
	}

	@Override
	protected final Double rangeValue() {
		return minValue() + (random.nextDouble() * range());
	}

	@Override
	protected final Double singleValue() {
		return random.nextDouble();
	}

	@Override
	protected String stringify(Double value) {
		return outputFormat.format(value);
	}

	@Override
	public final boolean isInitialized() {
		return true;
	}
}