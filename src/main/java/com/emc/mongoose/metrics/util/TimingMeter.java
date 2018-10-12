package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 10.10.18 */
interface TimingMeter
	extends Meter {

	long sum();

	long min();

	long max();

	double mean();

	void update(final long value);

	void update(final int value);
}
