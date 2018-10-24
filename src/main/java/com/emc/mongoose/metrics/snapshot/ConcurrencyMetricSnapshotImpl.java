package com.emc.mongoose.metrics.snapshot;

import java.util.List;

public class ActualConcurrencyMetricSnapshotImpl
extends NamedMetricSnapshotBase
implements ActualConcurrencyMetricSnapshot {

	private final long last;
	private final double mean;

	public ActualConcurrencyMetricSnapshotImpl(final String name, final long last, final long mean) {
		super(name);
		this.last = last;
		this.mean = mean;
	}

	public static ActualConcurrencyMetricSnapshot aggregate(final List<ActualConcurrencyMetricSnapshot> snapshots) {
		final int snapshotsCount = snapshots.size();
		if(snapshotsCount == 1) {
			return snapshots.get(0);
		}
		long last = 0;
		double meanSum = 0;
		ActualConcurrencyMetricSnapshot nextSnapshot;
		for(int i = 0; i < snapshotsCount; i++) {
			nextSnapshot = snapshots.get(i);
			last = nextSnapshot.last();
			meanSum += nextSnapshot.mean();
		}
		return new ActualConcurrencyMetricSnapshotImpl(
			snapshots.get(0).name(), last, (long) (meanSum / snapshotsCount)
		);
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
