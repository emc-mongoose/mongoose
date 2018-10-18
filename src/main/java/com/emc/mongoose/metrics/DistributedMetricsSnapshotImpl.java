package com.emc.mongoose.metrics;

import com.emc.mongoose.metrics.util.RateMetricSnapshot;
import com.emc.mongoose.metrics.util.TimingMetricSnapshot;

/**
 @author veronika K. */
public final class DistributedMetricsSnapshotImpl
extends MetricsSnapshotImpl
implements DistributedMetricsSnapshot {

	private final int nodeCount;

	public DistributedMetricsSnapshotImpl(
		final TimingMetricSnapshot durSnapshot,
		final TimingMetricSnapshot latSnapshot,
		final TimingMetricSnapshot actualConcurrencySnapshot,
		final RateMetricSnapshot failsSnapshot,
		final RateMetricSnapshot successSnapshot,
		final RateMetricSnapshot bytesSnapshot,
		final int nodeCount,
		final long elapsedTimeMillis
	) {
		super(
			durSnapshot, latSnapshot, actualConcurrencySnapshot, failsSnapshot, successSnapshot, bytesSnapshot,
			elapsedTimeMillis
		);
		this.nodeCount = nodeCount;
	}

	@Override
	public int nodeCount() {
		return nodeCount;
	}

	@Override
	public long elapsedTimeMillis() {
		return elapsedTimeMillis;
	}
}
