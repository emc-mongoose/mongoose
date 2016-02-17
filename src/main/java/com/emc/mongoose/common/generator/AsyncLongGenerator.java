package com.emc.mongoose.common.generator;

public final class AsyncLongGenerator
extends AsyncRangeGeneratorBase<Long> {

	public AsyncLongGenerator(final Long minValue, final Long maxValue) {
		super(minValue, maxValue);
	}

	public AsyncLongGenerator(final Long initialValue) {
		super(initialValue);
	}

	@Override
	protected final Long computeRange(final Long minValue, final Long maxValue) {
		return maxValue - minValue;
	}


	@Override
	protected final Long rangeValue() {
		return minValue() + (long) (random.nextDouble() * range());
	}

	@Override
	protected final Long singleValue() {
		return random.nextLong();
	}

	@Override
	public final boolean isInitialized() {
		return true;
	}
}
