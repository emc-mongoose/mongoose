package com.emc.mongoose.metrics.util;

import java.util.ArrayList;
import java.util.List;

/**
 @author veronika K. on 12.10.18 */
public interface TimingMetricSnapshot
extends SingleMetricSnapshot {

	long sum();

	long min();

	long max();

	long quantile(final double value);

	HistogramSnapshot histogramSnapshot();

	static TimingMetricSnapshot aggregate(final List<TimingMetricSnapshot> snapshots) {
		final int snapshotCount = snapshots.size();
		if(snapshotCount == 1) {
			return snapshots.get(0);
		} else {
			long countSum = 0;
			long sumOfSums = 0;
			long newMax = Long.MIN_VALUE;
			long newMin = Long.MAX_VALUE;
			final List<HistogramSnapshot> histogramSnapshots = new ArrayList<>();
			TimingMetricSnapshot nextSnapshot;
			for(int i = 0; i < snapshotCount; i ++) {
				nextSnapshot = snapshots.get(i);
				countSum += nextSnapshot.count();
				sumOfSums += nextSnapshot.sum();
				newMax = Math.max(newMax, nextSnapshot.max());
				newMin = Math.min(newMin, nextSnapshot.min());
				histogramSnapshots.add(nextSnapshot.histogramSnapshot());
			}
			return new TimingMetricSnapshotImpl(
				sumOfSums, countSum, newMin, newMax, ((double) sumOfSums) / countSum,
				HistogramSnapshot.aggregate(histogramSnapshots), snapshots.size() > 0 ? snapshots.get(0).name() : ""
			);
		}
	}
}
