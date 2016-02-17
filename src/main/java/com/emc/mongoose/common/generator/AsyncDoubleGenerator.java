package com.emc.mongoose.common.generator;

public class AsyncDoubleGenerator
extends AsyncRangeGeneratorBase<Double> {

	public AsyncDoubleGenerator(Double minValue, Double maxValue) {
		super(minValue, maxValue);
	}

	public AsyncDoubleGenerator(Double initialValue) {
		super(initialValue);
	}

	@Override
	protected Double computeRange(Double minValue, Double maxValue) {
		return maxValue - minValue;
	}

	@Override
	protected Double rangeValue() {
		return minValue() + (random.nextDouble() * range());
	}

	@Override
	protected Double singleValue() {
		return random.nextDouble();
	}

	@Override
	public boolean isInitialized() {
		return true;
	}
}
