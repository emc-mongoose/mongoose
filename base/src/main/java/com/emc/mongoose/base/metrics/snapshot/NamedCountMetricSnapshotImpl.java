package com.emc.mongoose.base.metrics.snapshot;

public class NamedCountMetricSnapshotImpl extends NamedMetricSnapshotBase
				implements CountMetricSnapshot, NamedMetricSnapshot {

	protected final long count;

	public NamedCountMetricSnapshotImpl(final String name, final long count) {
		super(name);
		this.count = count;
	}

	@Override
	public final long count() {
		return count;
	}
}
