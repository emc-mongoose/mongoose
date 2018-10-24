package com.emc.mongoose.metrics.snapshot;

/**
 @author veronika K. on 03.10.18 */
public interface HistogramSnapshot
extends LongLastMetricSnapshot {

	long quantile(final double quantile);

	long[] values();

	long last();
}
