package com.emc.mongoose.metrics;

import com.emc.mongoose.metrics.util.HistogramSnapshotImpl;

/**
 @author veronika K. */
public final class DistributedMetricsSnapshotImpl
	extends MetricsSnapshotImpl
	implements DistributedMetricsSnapshot {

	private final int nodeCount;

	public DistributedMetricsSnapshotImpl(
		final long countSucc, final double succRateLast, final long countFail, final double failRateLast,
		final long countByte, final double byteRateLast, final long startTimeMillis, final long elapsedTimeMillis,
		final int actualConcurrencyLast, final double actualConcurrencyMean, final int concurrencyLimit,
		final int nodeCount, final HistogramSnapshotImpl durSnapshot, final HistogramSnapshotImpl latSnapshot
	) {
		super(
			countSucc, succRateLast, countFail, failRateLast, countByte, byteRateLast, startTimeMillis,
			elapsedTimeMillis, actualConcurrencyLast, actualConcurrencyMean, concurrencyLimit,
			durSnapshot, latSnapshot
		);
		this.nodeCount = nodeCount;
	}

	@Override
	public int nodeCount() {
		return nodeCount;
	}
}
