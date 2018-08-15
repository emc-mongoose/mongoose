package com.emc.mongoose.metrics;

public final class DistributedMetricsSnapshotImpl
extends MetricsSnapshotImpl
implements DistributedMetricsSnapshot {

	private final int nodeCount;

	public DistributedMetricsSnapshotImpl(
		final long countSucc, final double succRateLast, final long countFail, final double failRateLast,
		final long countByte, final double byteRateLast, final long startTimeMillis, final long elapsedTimeMillis,
		final int actualConcurrencyLast, final double actualConcurrencyMean, final int concurrencyLimit,
		final long sumDur, final long sumLat, final int nodeCount, final com.codahale.metrics.Snapshot durSnapshot,
		final com.codahale.metrics.Snapshot latSnapshot
	) {
		super(
			countSucc, succRateLast, countFail, failRateLast, countByte, byteRateLast, startTimeMillis,
			elapsedTimeMillis, actualConcurrencyLast, actualConcurrencyMean, concurrencyLimit, sumDur, sumLat,
			durSnapshot, latSnapshot
		);
		this.nodeCount = nodeCount;
	}

	@Override
	public int nodeCount() {
		return nodeCount;
	}
}
