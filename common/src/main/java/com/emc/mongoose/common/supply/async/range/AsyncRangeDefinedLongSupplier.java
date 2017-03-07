package com.emc.mongoose.common.supply.async.range;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.supply.async.range.AsyncRangeDefinedSupplierBase;

public final class AsyncRangeDefinedLongSupplier
extends AsyncRangeDefinedSupplierBase<Long> {

	public AsyncRangeDefinedLongSupplier(final Long minValue, final Long maxValue)
	throws OmgDoesNotPerformException {
		super(minValue, maxValue);
	}

	public AsyncRangeDefinedLongSupplier(final Long initialValue)
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
