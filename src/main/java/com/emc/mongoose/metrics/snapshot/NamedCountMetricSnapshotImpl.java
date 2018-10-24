package com.emc.mongoose.metrics.snapshot;

public class CountMetricSnapshotImpl
extends NamedMetricSnapshotBase
implements CountMetricSnapshot, NamedMetricSnapshot {

	protected final long count;

	public CountMetricSnapshotImpl(final String name, final long count) {
		super(name);
		this.count = count;
	}

	@Override
	public final long count() {
		return count;
	}
}
