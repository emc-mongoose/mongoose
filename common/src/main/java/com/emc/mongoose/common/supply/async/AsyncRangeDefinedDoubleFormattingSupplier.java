package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public final class AsyncRangeDefinedDoubleFormattingSupplier
extends AsyncRangeDefinedSupplierBase<Double> {
	
	private final NumberFormat format;

	public AsyncRangeDefinedDoubleFormattingSupplier(
		final long seed, final double minValue, final double maxValue, final String formatString
	) throws OmgDoesNotPerformException {
		super(seed, minValue, maxValue);
		this.format = formatString == null || formatString.isEmpty() ?
			null : new DecimalFormat(formatString);
	}
	
	@Override
	protected final Double computeRange(final Double minValue, final Double maxValue) {
		return maxValue - minValue;
	}

	@Override
	protected final Double rangeValue() {
		return minValue() + (rnd.nextDouble() * range());
	}

	@Override
	protected final Double singleValue() {
		return rnd.nextDouble();
	}

	@Override
	protected final String toString(Double value) {
		return format == null ? value.toString() : format.format(value);
	}
}
