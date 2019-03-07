package com.emc.mongoose.base.metrics.snapshot;

/** @author veronika K. */
public final class DistributedAllMetricsSnapshotImpl extends AllMetricsSnapshotImpl
				implements DistributedAllMetricsSnapshot {

	private final int nodeCount;

	public DistributedAllMetricsSnapshotImpl(
					final TimingMetricSnapshot durSnapshot,
					final TimingMetricSnapshot latSnapshot,
					final ConcurrencyMetricSnapshot actualConcurrencySnapshot,
					final RateMetricSnapshot failsSnapshot,
					final RateMetricSnapshot successSnapshot,
					final RateMetricSnapshot bytesSnapshot,
					final int nodeCount,
					final long elapsedTimeMillis) {
		super(
						durSnapshot,
						latSnapshot,
						actualConcurrencySnapshot,
						failsSnapshot,
						successSnapshot,
						bytesSnapshot,
						elapsedTimeMillis);
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
