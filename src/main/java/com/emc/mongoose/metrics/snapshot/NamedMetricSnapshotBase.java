package com.emc.mongoose.metrics.snapshot;

public abstract class MetricSnapshotBase
implements NamedMetricSnapshot {

	private final String name;

	protected MetricSnapshotBase(final String name) {
		this.name = name;
	}

	@Override
	public final String name() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
