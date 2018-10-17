package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 12.10.18 */
public class TimingMetricSnapshotImpl
implements TimingMetricSnapshot {

	private final long sum;
	private final long count;
	private final long min;
	private final long max;
	private final double mean;
	private final HistogramSnapshot histogramSnapshot;
	private final String metricName;

	public TimingMetricSnapshotImpl(
		final long sum, final long count, final long min, final long max, final double mean,
		final HistogramSnapshot histogramSnapshot, final String metricName
	) {
		this.sum = sum;
		this.count = count;
		this.min = min;
		this.max = max;
		this.mean = mean;
		this.histogramSnapshot = histogramSnapshot;
		this.metricName = metricName;
	}

	@Override
	public long sum() {
		return sum;
	}

	@Override
	public long min() {
		return min;
	}

	@Override
	public long max() {
		return max;
	}

	@Override
	public long quantile(final double value) {
		return histogramSnapshot.quantile(value);
	}

	@Override
	public HistogramSnapshot histogramSnapshot() {
		return histogramSnapshot;
	}

	@Override
	public String name() {
		return metricName;
	}

	@Override
	public double mean() {
		return mean;
	}

	@Override
	public long count() {
		return count;
	}
}
