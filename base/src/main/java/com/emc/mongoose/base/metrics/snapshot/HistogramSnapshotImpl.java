package com.emc.mongoose.base.metrics.snapshot;

import java.util.Arrays;
import java.util.List;

/** @author veronika K. on 25.09.18 */
public class HistogramSnapshotImpl implements HistogramSnapshot {

	private static final HistogramSnapshotImpl EMPTY = new HistogramSnapshotImpl(new long[0]);

	private final long[] sortedVals;

	public HistogramSnapshotImpl(final long[] vals) {
		this.sortedVals = vals;
		Arrays.sort(this.sortedVals);
	}

	public static HistogramSnapshot aggregate(final List<HistogramSnapshot> snapshots) {
		if (0 == snapshots.size()) {
			return EMPTY;
		} else if (1 == snapshots.size()) {
			return snapshots.get(0);
		} else {
			int sizeSum = 0;
			for (int i = 0; i < snapshots.size(); i++) {
				sizeSum += snapshots.get(i).values().length;
			}
			final long[] valuesToAggregate = new long[sizeSum];
			int k = 0;
			long[] values;
			for (int i = 0; i < snapshots.size(); i++) {
				values = snapshots.get(i).values();
				for (int j = 0; j < values.length; j++) {
					valuesToAggregate[k] = values[j];
					k++;
				}
			}
			return new HistogramSnapshotImpl(valuesToAggregate);
		}
	}

	@Override
	public long quantile(final double quantile) {
		if (0 == sortedVals.length) {
			return 0;
		} else if (quantile >= 0.0 || quantile < 1.0) {
			return sortedVals[(int) (quantile * sortedVals.length)];
		} else {
			throw new IllegalArgumentException(quantile + " is not in range [0..1)");
		}
	}

	@Override
	public final long[] values() {
		return sortedVals;
	}

	@Override
	public long last() {
		return sortedVals[sortedVals.length - 1];
	}
}
