package com.emc.mongoose.metrics.util;

/**
 @author veronika K. on 12.10.18 */
public class RateMetricSnapshotImpl
implements RateMetricSnapshot {

	private final double lastRate;
	private final double meanRate;
	private final String metricName;
	private final long count;

	public RateMetricSnapshotImpl(
		final double lastRate, final double meanRate, final String metricName, final long count
	) {
		this.lastRate = lastRate;
		this.meanRate = meanRate;
		this.metricName = metricName;
		this.count = count;
	}

	@Override
	public double last() {
		return lastRate;
	}

	@Override
	public String name() {
		return metricName;
	}

	@Override
	public double mean() {
		return meanRate;
	}

	@Override
	public long count() {
		return count;
	}
}
