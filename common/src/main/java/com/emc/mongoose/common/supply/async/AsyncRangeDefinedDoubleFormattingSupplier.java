package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

import java.text.DecimalFormat;
import java.text.Format;

public final class AsyncRangeDefinedDoubleFormattingSupplier
extends AsyncFormattingRangeDefinedSupplierBase<Double> {

	public AsyncRangeDefinedDoubleFormattingSupplier(
		final Double minValue, final Double maxValue, final String formatString
	) throws OmgDoesNotPerformException {
		super(minValue, maxValue, formatString);
	}

	public AsyncRangeDefinedDoubleFormattingSupplier(final Double initialValue, final String formatString)
	throws OmgDoesNotPerformException {
		super(initialValue, formatString);
	}

	/**
	 *
	 * @param formatString - a pattern for DecimalFormat. It should match a pattern,
	 *                        that described here
	 *                        https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html
	 *                        (see Special Pattern Characters paragraph for examples)
	 * @return a suitable formatter for numbers of a double type
	 */
	@Override
	protected final Format getFormatterInstance(String formatString) {
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
	protected final String toString(Double value) {
		return outputFormat().format(value);
	}
}
