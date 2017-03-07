package com.emc.mongoose.common.supply.async;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;

public final class AsyncRangeDefinedLongFormattingSupplier
extends AsyncRangeDefinedSupplierBase<Long> {

	public AsyncRangeDefinedLongFormattingSupplier(final Long minValue, final Long maxValue)
	throws OmgDoesNotPerformException {
		super(minValue, maxValue);
	}

	public AsyncRangeDefinedLongFormattingSupplier(final Long initialValue)
	throws OmgDoesNotPerformException {
		super(initialValue);
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
	protected String stringify(Long value) {
		return value.toString();
	}
}
