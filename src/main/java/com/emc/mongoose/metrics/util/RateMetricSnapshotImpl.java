package com.emc.mongoose.metrics.util;

import java.util.List;

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

	public RateMetricSnapshotImpl(final List<RateMetricSnapshot> snapshots) {
		double newLastRate = 0;
		double newMeanRate = 0;
		long newCount = 0;
		final int snapshotsCount = snapshots.size();
		for(final RateMetricSnapshot snapshot : snapshots) {
			newCount += snapshot.count();
			newLastRate += snapshot.last();
			newMeanRate += snapshot.mean();
		}
		this.lastRate = newLastRate / snapshotsCount;
		this.meanRate = newMeanRate / snapshotsCount;
		this.count = newCount;
		this.metricName = snapshots.get(0).name();
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
