package com.emc.mongoose.metrics.snapshot;

import java.io.Serializable;

/**
 @author veronika K. on 03.10.18 */
public interface HistogramSnapshot
extends Serializable {

	long quantile(final double quantile);

	int count();

	long[] values();

	long max();

	long min();

	long mean();

	long sum();

	long last();
}
