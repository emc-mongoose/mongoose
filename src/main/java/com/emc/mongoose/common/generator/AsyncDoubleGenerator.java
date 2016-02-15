package com.emc.mongoose.common.generator;

public class AsyncDoubleGenerator extends AsyncNumberGeneratorBase<Double> {

	public AsyncDoubleGenerator(Double minValue, Double maxValue) {
		super(minValue, maxValue);
	}

	public AsyncDoubleGenerator(Double initialValue) {
		super(initialValue);
	}

	@Override
	Double computeRange(Double minValue, Double maxValue) {
		return maxValue - minValue;
	}

	@Override
	Double rangeValue() {
		return minValue() + (random.nextDouble() * range());
	}

	@Override
	Double singleValue() {
		return random.nextDouble();
	}

}
