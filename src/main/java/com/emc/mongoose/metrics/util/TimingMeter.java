package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 10.10.18 */
public interface TimingMeter<S extends SingleMetricSnapshot>
extends Meter<S> {

	long sum();

	long min();

	long max();

	double mean();

	void update(final long value);
}
