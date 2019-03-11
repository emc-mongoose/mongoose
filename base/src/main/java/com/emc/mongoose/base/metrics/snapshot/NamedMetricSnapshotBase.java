package com.emc.mongoose.base.metrics.snapshot;

public abstract class NamedMetricSnapshotBase implements NamedMetricSnapshot {

	private final String name;

	protected NamedMetricSnapshotBase(final String name) {
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
