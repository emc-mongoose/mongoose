package com.emc.mongoose.metrics.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

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
			final LongAdder countSum = new LongAdder();
			final LongAdder sumOfSums = new LongAdder();
			final AtomicLong newMax = new AtomicLong(Long.MIN_VALUE);
			final AtomicLong newMin = new AtomicLong(Long.MAX_VALUE);
			final List<HistogramSnapshot> histogramSnapshots = new ArrayList<>();
			snapshots.parallelStream().forEach(s -> {
				countSum.add(s.count());
				sumOfSums.add(s.sum());
				newMin.set(Math.min(newMin.get(), s.min()));
				newMax.set(Math.max(newMax.get(), s.max()));
				histogramSnapshots.add(s.histogramSnapshot());
			});
			return new TimingMetricSnapshotImpl(
				sumOfSums.longValue(), countSum.longValue(), newMin.get(), newMax.get(),
				sumOfSums.doubleValue() / countSum.longValue(),
				HistogramSnapshot.aggregate(histogramSnapshots), snapshotCount > 0 ? snapshots.get(0).name() : ""
			);
		}
	}
}
