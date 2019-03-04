package com.emc.mongoose.base.metrics.snapshot;

import java.util.List;

public class ConcurrencyMetricSnapshotImpl extends NamedMetricSnapshotBase
				implements ConcurrencyMetricSnapshot {

	private final long last;
	private final double mean;

	public ConcurrencyMetricSnapshotImpl(final String name, final long last, final double mean) {
		super(name);
		this.last = last;
		this.mean = mean;
	}

	public static ConcurrencyMetricSnapshot aggregate(
					final List<ConcurrencyMetricSnapshot> snapshots) {
		final int snapshotsCount = snapshots.size();
		if (snapshotsCount == 1) {
			return snapshots.get(0);
		}
		long lastSum = 0;
		double meanSum = 0;
		ConcurrencyMetricSnapshot nextSnapshot;
		for (int i = 0; i < snapshotsCount; i++) {
			nextSnapshot = snapshots.get(i);
			lastSum = nextSnapshot.last();
			meanSum += nextSnapshot.mean();
		}
		return new ConcurrencyMetricSnapshotImpl(snapshots.get(0).name(), lastSum, meanSum);
	}

	@Override
	public final long last() {
		return last;
	}

	@Override
	public final double mean() {
		return mean;
	}
}
