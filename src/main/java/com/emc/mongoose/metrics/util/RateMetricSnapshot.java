package com.emc.mongoose.metrics.util;

import java.util.List;

/**
 @author veronika K. on 12.10.18 */
public interface RateMetricSnapshot
	extends SingleMetricSnapshot {

	double last();

	static RateMetricSnapshot aggregate(final List<RateMetricSnapshot> snapshots) {
		final int snapshotsCount = snapshots.size();
		if(snapshotsCount == 0) {
			return new RateMetricSnapshotImpl(0, 0, "", 0);
		}
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
}
