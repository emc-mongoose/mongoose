package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public final class AsyncRangeDefinedLongFormattingSupplier
extends AsyncRangeDefinedSupplierBase<Long> {
	
	private final NumberFormat format;
	
	public AsyncRangeDefinedLongFormattingSupplier()
	throws OmgDoesNotPerformException {
		this(Long.MIN_VALUE, Long.MAX_VALUE, null);
	}
	
	public AsyncRangeDefinedLongFormattingSupplier(final long minValue, final long maxValue)
	throws OmgDoesNotPerformException {
		this(minValue, maxValue, null);
	}
	
	public AsyncRangeDefinedLongFormattingSupplier(final String formatStr)
	throws OmgDoesNotPerformException {
		this(Long.MIN_VALUE, Long.MAX_VALUE, formatStr);
	}
	
	public AsyncRangeDefinedLongFormattingSupplier(
		final long minValue, final long maxValue, final String formatStr
	) throws OmgDoesNotPerformException {
		super(minValue, maxValue);
		this.format = formatStr == null || formatStr.isEmpty() ?
			null : new DecimalFormat(formatStr);
	}

	@Override
	protected final Long computeRange(final Long minValue, final Long maxValue) {
		return maxValue - minValue + 1;
	}

	@Override
	protected final Long rangeValue() {
		return minValue() + random.nextLong(range());
	}

	@Override
	protected final Long singleValue() {
		return random.nextLong();
	}

	@Override
	protected String toString(final Long value) {
		return format == null ? value.toString() : format.format(value);
	}
}
