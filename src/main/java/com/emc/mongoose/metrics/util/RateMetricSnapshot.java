package com.emc.mongoose.metrics.util;

import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 @author veronika K. on 12.10.18 */
public interface RateMetricSnapshot
	extends SingleMetricSnapshot {

	double last();

	static RateMetricSnapshot aggregate(final List<RateMetricSnapshot> snapshots) {
		final int snapshotsCount = snapshots.size();
		if(snapshotsCount == 1) {
			return snapshots.get(0);
		} else {
			final DoubleAdder lastRateSum = new DoubleAdder();
			final DoubleAdder meanRateSum = new DoubleAdder();
			final LongAdder countSum = new LongAdder();
			snapshots.parallelStream().forEach(s -> {
					countSum.add(s.count());
					lastRateSum.add(s.last());
					meanRateSum.add(s.mean());
				}
			);
			return new RateMetricSnapshotImpl(
				lastRateSum.doubleValue() / snapshotsCount,
				meanRateSum.doubleValue() / snapshotsCount,
				snapshotsCount > 0 ? snapshots.get(0).name() : "", countSum.longValue()
			);
		}
	}
}
