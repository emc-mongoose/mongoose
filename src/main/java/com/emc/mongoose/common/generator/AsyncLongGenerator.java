package com.emc.mongoose.common.generator;

public class AsyncLongGenerator
extends AsyncRangeGeneratorBase<Long> {

	public AsyncLongGenerator(Long minValue, Long maxValue) {
		super(minValue, maxValue);
	}

	public AsyncLongGenerator(Long initialValue) {
		super(initialValue);
	}

	@Override
	protected Long computeRange(Long minValue, Long maxValue) {
		return maxValue - minValue;
	}

	@Override
	protected Long rangeValue() {
		return minValue() + (long) (random.nextDouble() * range());
	}

	@Override
	protected Long singleValue() {
		return random.nextLong();
	}

	@Override
	public boolean isInitialized() {
		return true;
	}
}
