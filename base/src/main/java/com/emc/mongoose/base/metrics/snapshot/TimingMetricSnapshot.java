package com.emc.mongoose.base.metrics.snapshot;

/** @author veronika K. on 12.10.18 */
public interface TimingMetricSnapshot
				extends CountMetricSnapshot, NamedMetricSnapshot, MeanMetricSnapshot {

	long sum();

	long min();

	long max();

	long quantile(final double value);

	HistogramSnapshot histogramSnapshot();
}
