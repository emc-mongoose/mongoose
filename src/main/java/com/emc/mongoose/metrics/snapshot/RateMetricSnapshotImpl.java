package com.emc.mongoose.metrics.snapshot;

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

	public static RateMetricSnapshot aggregate(final List<RateMetricSnapshot> snapshots) {
		final int snapshotsCount = snapshots.size();
		if(snapshotsCount == 1) {
			return snapshots.get(0);
		}
		double lastRateSum = 0;
		double meanRateSum = 0;
		long countSum = 0;
		RateMetricSnapshot nextSnapshot;
		for(int i = 0; i < snapshotsCount; i++) {
			nextSnapshot = snapshots.get(i);
			countSum += nextSnapshot.count();
			lastRateSum += nextSnapshot.last();
			meanRateSum += nextSnapshot.mean();
		}
		return new RateMetricSnapshotImpl(
			lastRateSum / snapshotsCount, meanRateSum / snapshotsCount, snapshots.get(0).name(), countSum
		);
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
