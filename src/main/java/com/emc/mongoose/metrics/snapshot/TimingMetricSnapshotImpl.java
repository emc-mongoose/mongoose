package com.emc.mongoose.metrics.snapshot;

import java.util.ArrayList;
import java.util.List;

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

	public static TimingMetricSnapshot aggregate(final List<TimingMetricSnapshot> snapshots) {
		final int snapshotCount = snapshots.size();
		if(snapshotCount == 1) {
			return snapshots.get(0);
		}
		long countSum = 0;
		long sumOfSums = 0;
		long newMax = Long.MIN_VALUE;
		long newMin = Long.MAX_VALUE;
		final List<HistogramSnapshot> histogramSnapshots = new ArrayList<>();
		TimingMetricSnapshot nextSnapshot;
		for(int i = 0; i < snapshotCount; ++ i) {
			nextSnapshot = snapshots.get(i);
			countSum += nextSnapshot.count();
			sumOfSums += nextSnapshot.sum();
			newMax = Math.max(newMax, nextSnapshot.max());
			newMin = Math.min(newMin, nextSnapshot.min());
			histogramSnapshots.add(nextSnapshot.histogramSnapshot());
		}
		if(sumOfSums == 0) {
			newMin = 0;
			newMax = 0;
		}
		final double newMean = countSum > 0 ? ((double) sumOfSums) / countSum : 0;
		return new TimingMetricSnapshotImpl(
			sumOfSums, countSum, newMin, newMax, newMean, HistogramSnapshotImpl.aggregate(histogramSnapshots),
			snapshots.get(0).name()
		);
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
