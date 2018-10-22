package com.emc.mongoose.metrics.snapshot;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.LongStream;

/**
 @author veronika K. on 25.09.18 */
public class HistogramSnapshotImpl
implements HistogramSnapshot {

	private static final HistogramSnapshotImpl EMPTY = new HistogramSnapshotImpl(new long[0]);

	private final long[] sortedVals;

	public HistogramSnapshotImpl(final long[] vals) {
		this.sortedVals = vals;
		Arrays.sort(this.sortedVals);
	}

	public static HistogramSnapshot aggregate(final List<HistogramSnapshot> snapshots) {
		if(0 == snapshots.size()) {
			return EMPTY;
		} else if(1 == snapshots.size()) {
			return snapshots.get(0);
		} else {
			final SortedSet<Long> sortedVals = new TreeSet<>();
			snapshots
				.stream()
				.map(HistogramSnapshot::values)
				.forEach(values -> LongStream.of(values).forEach(sortedVals::add));
			return new HistogramSnapshotImpl(sortedVals.stream().mapToLong(x -> x).toArray());
		}
	}

	@Override
	public long quantile(final double quantile) {
		if(0 == sortedVals.length) {
			return 0;
		} else if(quantile >= 0.0 || quantile < 1.0) {
			return sortedVals[(int) (quantile * sortedVals.length)];
		} else {
			throw new IllegalArgumentException(quantile + " is not in range [0..1)");
		}
	}

	@Override
	public int count() {
		return sortedVals.length;
	}

	@Override
	public long[] values() {
		return sortedVals;
	}

	@Override
	public long max() {
		if(sortedVals.length == 0) {
			return 0;
		}
		return sortedVals[sortedVals.length - 1];
	}

	@Override
	public long min() {
		if(sortedVals.length == 0) {
			return 0;
		}
		return sortedVals[0];
	}

	@Override
	public long mean() {
		if(sortedVals.length == 0) {
			return 0;
		}
		return sum() / sortedVals.length;
	}

	@Override
	public long sum() {
		long sum = 0;
		for(int i = 0; i < sortedVals.length; ++ i) {
			sum += sortedVals[i];
		}
		return sum;
	}

	@Override
	public long last() {
		return sortedVals[sortedVals.length - 1];
	}
}
