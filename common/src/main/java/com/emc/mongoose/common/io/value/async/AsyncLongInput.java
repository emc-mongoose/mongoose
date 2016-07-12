package com.emc.mongoose.common.io.value.async;

public final class AsyncLongInput
extends AsyncRangeInputBase<Long> {

	public
	AsyncLongInput(final Long minValue, final Long maxValue) {
		super(minValue, maxValue);
	}

	public
	AsyncLongInput(final Long initialValue) {
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
