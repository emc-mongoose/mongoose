package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 03.10.18 */
public interface HistogramSnapshot {

	long quantile(final double quantile);

	int count();

	long[] values();

	long max();

	long min();

	long mean();

	long sum();
}
