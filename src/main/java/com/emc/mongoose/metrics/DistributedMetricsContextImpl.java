package com.emc.mongoose.metrics;

import com.emc.mongoose.item.op.OpType;
import com.github.akurilov.commons.system.SizeInBytes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class DistributedMetricsContextImpl<S extends DistributedMetricsSnapshotImpl>
	extends MetricsContextBase<S>
	implements DistributedMetricsContext<S> {

	private final IntSupplier nodeCountSupplier;
	private final Supplier<List<MetricsSnapshot>> snapshotsSupplier;
	private final boolean avgPersistFlag;
	private final boolean sumPersistFlag;
	private final boolean perfDbResultsFileFlag;
	private volatile long prevElapsedTime = 0;
	private volatile DistributedMetricsListener metricsListener = null;

	public DistributedMetricsContextImpl(
		final String id, final OpType opType, final IntSupplier nodeCountSupplier, final int concurrencyLimit,
		final int concurrencyThreshold, final SizeInBytes itemDataSize, final int updateIntervalSec,
		final boolean stdOutColorFlag, final boolean avgPersistFlag, final boolean sumPersistFlag,
		final boolean perfDbResultsFileFlag, final Supplier<List<MetricsSnapshot>> snapshotsSupplier
	) {
		super(
			id, opType, concurrencyLimit, concurrencyThreshold, itemDataSize, stdOutColorFlag,
			TimeUnit.SECONDS.toMillis(updateIntervalSec)
		);
		this.nodeCountSupplier = nodeCountSupplier;
		this.snapshotsSupplier = snapshotsSupplier;
		this.avgPersistFlag = avgPersistFlag;
		this.sumPersistFlag = sumPersistFlag;
		this.perfDbResultsFileFlag = perfDbResultsFileFlag;
	}

	@Override
	public void markSucc(final long bytes, final long duration, final long latency) {
	}

	@Override
	public void markPartSucc(final long bytes, final long duration, final long latency) {
	}

	@Override
	public void markSucc(final long count, final long bytes, final long[] durationValues, final long[] latencyValues) {
	}

	@Override
	public void markPartSucc(final long bytes, final long[] durationValues, final long[] latencyValues) {
	}

	@Override
	public void markFail() {
	}

	@Override
	public void markFail(final long count) {
	}

	@Override
	public void markElapsedTime(final long millis) {
	}

	@Override
	public int nodeCount() {
		return nodeCountSupplier.getAsInt();
	}

	@Override
	public boolean avgPersistEnabled() {
		return avgPersistFlag;
	}

	@Override
	public boolean sumPersistEnabled() {
		return sumPersistFlag;
	}

	@Override
	public boolean perfDbResultsFileEnabled() {
		return perfDbResultsFileFlag;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void refreshLastSnapshot() {
		final List<MetricsSnapshot> snapshots = snapshotsSupplier.get();
		long countSucc = 0;
		double succRateLast = 0;
		long countFail = 0;
		double failRateLast = 0;
		long countByte = 0;
		double byteRateLast = 0;
		int actualConcurrencyLast = 0;
		double actualConcurrencyMean = 0;
		final List<HistogramSnapshotImpl> durSnapshots = new ArrayList<>();
		final List<HistogramSnapshotImpl> latSnapshots = new ArrayList<>();
		for(final MetricsSnapshot snapshot : snapshots) {
			countSucc += snapshot.succCount();
			succRateLast += snapshot.succRateLast();
			countFail += snapshot.failCount();
			failRateLast += snapshot.failRateLast();
			countByte += snapshot.byteCount();
			byteRateLast += snapshot.byteRateLast();
			actualConcurrencyLast += snapshot.actualConcurrencyLast();
			actualConcurrencyMean += snapshot.actualConcurrencyMean();
			durSnapshots.add(new HistogramSnapshotImpl(snapshot.durationValues()));
			latSnapshots.add(new HistogramSnapshotImpl(snapshot.latencyValues()));
		}
		final HistogramSnapshotImpl durSnapshot = new HistogramSnapshotImpl(durSnapshots);
		final HistogramSnapshotImpl latSnapshot = new HistogramSnapshotImpl(latSnapshots);
		final long currentTimeMillis = System.currentTimeMillis();
		final long tsStart = startTimeStamp();
		final long currElapsedTime = tsStart > 0 ? currentTimeMillis - tsStart : 0;
		lastSnapshot = (S) new DistributedMetricsSnapshotImpl(
			countSucc, succRateLast, countFail, failRateLast, countByte, byteRateLast, tsStart,
			prevElapsedTime + currElapsedTime, actualConcurrencyLast, actualConcurrencyMean, concurrencyLimit,
			nodeCountSupplier.getAsInt(), durSnapshot, latSnapshot
		);
		if(metricsListener != null) {
			metricsListener.notify(lastSnapshot);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.refreshLastSnapshot();
		}
	}

	@Override
	protected DistributedMetricsContextImpl<S> newThresholdMetricsContext() {
		return new DistributedMetricsContextImpl(
			id, opType, nodeCountSupplier, concurrencyLimit, 0, itemDataSize,
			(int) TimeUnit.MILLISECONDS.toSeconds(outputPeriodMillis), stdOutColorFlag, avgPersistFlag,
			sumPersistFlag, perfDbResultsFileFlag, snapshotsSupplier
		);
	}

	@Override
	public void metricsListener(final DistributedMetricsListener metricsListener) {
		this.metricsListener = metricsListener;
	}

	@Override
	public long transferSizeSum() {
		return lastSnapshot.byteCount();
	}

	@Override
	public final boolean equals(final Object other) {
		if(null == other) {
			return false;
		}
		if(other instanceof MetricsContext) {
			return 0 == compareTo((MetricsContext) other);
		} else {
			return false;
		}
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "(" + opType.name() + '-' + concurrencyLimit + "x" + nodeCount() + "@" + id
			+ ")";
	}

	@Override
	public final void close() {
		prevElapsedTime = System.currentTimeMillis() - startTimeStamp();
		super.close();
	}
}
