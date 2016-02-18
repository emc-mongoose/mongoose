package com.emc.mongoose.common.generator;

public final class AsyncDoubleGenerator
extends AsyncRangeGeneratorBase<Double> {

	public AsyncDoubleGenerator(final Double minValue, final Double maxValue) {
		super(minValue, maxValue);
	}

	public AsyncDoubleGenerator(final Double initialValue) {
		super(initialValue);
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
	public final boolean isInitialized() {
		return true;
	}
}
