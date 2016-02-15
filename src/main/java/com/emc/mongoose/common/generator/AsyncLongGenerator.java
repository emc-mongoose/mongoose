package com.emc.mongoose.common.generator;

public class AsyncLongGenerator extends AsyncNumberGeneratorBase<Long> {

	public AsyncLongGenerator(Long minValue, Long maxValue) {
		super(minValue, maxValue);
	}

	public AsyncLongGenerator(Long initialValue) {
		super(initialValue);
	}

	@Override
	Long computeRange(Long minValue, Long maxValue) {
		return maxValue - minValue;
	}

	@Override
	Long rangeValue() {
		return minValue() + (long) (random.nextDouble() * range());
	}

	@Override
	Long singleValue() {
		return random.nextLong();
	}

}
