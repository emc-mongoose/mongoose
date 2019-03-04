package com.emc.mongoose.base.metrics.snapshot;

import java.util.ArrayList;
import java.util.List;

/** @author veronika K. on 12.10.18 */
public class TimingMetricSnapshotImpl extends NamedCountMetricSnapshotImpl
				implements TimingMetricSnapshot {

	private final long sum;
	private final long min;
	private final long max;
	private final double mean;
	private final HistogramSnapshot histogramSnapshot;

	public TimingMetricSnapshotImpl(
					final long sum,
					final long count,
					final long min,
					final long max,
					final double mean,
					final HistogramSnapshot histogramSnapshot,
					final String metricName) {
		super(metricName, count);
		this.sum = sum;
		this.min = min;
		this.max = max;
		this.mean = mean;
		this.histogramSnapshot = histogramSnapshot;
	}

	public static TimingMetricSnapshot aggregate(final List<TimingMetricSnapshot> snapshots) {
		final int snapshotCount = snapshots.size();
		if (snapshotCount == 1) {
			return snapshots.get(0);
		}
		long countSum = 0;
		long sumOfSums = 0;
		long newMax = Long.MIN_VALUE;
		long newMin = Long.MAX_VALUE;
		final List<HistogramSnapshot> histogramSnapshots = new ArrayList<>(snapshotCount);
		TimingMetricSnapshot nextSnapshot;
		for (int i = 0; i < snapshotCount; ++i) {
			nextSnapshot = snapshots.get(i);
			countSum += nextSnapshot.count();
			sumOfSums += nextSnapshot.sum();
			newMax = Math.max(newMax, nextSnapshot.max());
			newMin = Math.min(newMin, nextSnapshot.min());
			histogramSnapshots.add(nextSnapshot.histogramSnapshot());
		}
		if (sumOfSums == 0) {
			newMin = 0;
			newMax = 0;
		}
		final double newMean = countSum > 0 ? ((double) sumOfSums) / countSum : 0;
		return new TimingMetricSnapshotImpl(
						sumOfSums,
						countSum,
						newMin,
						newMax,
						newMean,
						HistogramSnapshotImpl.aggregate(histogramSnapshots),
						snapshots.get(0).name());
	}

	@Override
	public final long sum() {
		return sum;
	}

	@Override
	public final long min() {
		return min;
	}

	@Override
	public final long max() {
		return max;
	}

	@Override
	public long quantile(final double value) {
		return histogramSnapshot.quantile(value);
	}

	@Override
	public final HistogramSnapshot histogramSnapshot() {
		return histogramSnapshot;
	}

	@Override
	public final double mean() {
		return mean;
	}
}
