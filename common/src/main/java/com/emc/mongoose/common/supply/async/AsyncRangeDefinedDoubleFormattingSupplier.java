package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public final class AsyncRangeDefinedDoubleFormattingSupplier
extends AsyncRangeDefinedSupplierBase<Double> {
	
	private final NumberFormat format;
	
	public AsyncRangeDefinedDoubleFormattingSupplier()
	throws OmgDoesNotPerformException {
		this(0, 1, null);
	}
	
	public AsyncRangeDefinedDoubleFormattingSupplier(final double minValue, final double maxValue)
	throws OmgDoesNotPerformException {
		this(minValue, maxValue, null);
	}
	
	public AsyncRangeDefinedDoubleFormattingSupplier(final String formatString)
	throws OmgDoesNotPerformException {
		this(0, 1, formatString);
	}
	
	public AsyncRangeDefinedDoubleFormattingSupplier(
		final double minValue, final double maxValue, final String formatString
	) throws OmgDoesNotPerformException {
		super(minValue, maxValue);
		this.format = formatString == null || formatString.isEmpty() ?
			null : new DecimalFormat(formatString);
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
		return format == null ? value.toString() : format.format(value);
	}
}
