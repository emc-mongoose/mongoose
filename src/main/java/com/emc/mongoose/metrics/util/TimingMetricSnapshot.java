package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 12.10.18 */
public interface TimingMetricSnapshot
	extends SingleMetricSnapshot {

	long sum();

	long min();

	long max();

	long quantile(final double value);

	HistogramSnapshot histogramSnapshot();
}
