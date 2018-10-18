package com.emc.mongoose.metrics.context;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsListener;
import com.emc.mongoose.metrics.DistributedMetricsSnapshotImpl;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.metrics.util.RateMetricSnapshot;
import com.emc.mongoose.metrics.util.TimingMetricSnapshot;

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
	private volatile DistributedMetricsListener metricsListener = null;

	public DistributedMetricsContextImpl(
		final String id, final OpType opType, final IntSupplier nodeCountSupplier, final int concurrencyLimit,
		final int concurrencyThreshold, final SizeInBytes itemDataSize, final int updateIntervalSec,
		final boolean stdOutColorFlag, final boolean avgPersistFlag, final boolean sumPersistFlag,
		final boolean perfDbResultsFileFlag, final Supplier<List<MetricsSnapshot>> snapshotsSupplier
	) {
		super(
			id, opType, concurrencyLimit, nodeCountSupplier.getAsInt(), concurrencyThreshold, itemDataSize,
			stdOutColorFlag, TimeUnit.SECONDS.toMillis(updateIntervalSec)
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
		final int snapshotsCount = snapshots.size();

		if(snapshotsCount > 0) { // do nothing otherwise

			final RateMetricSnapshot successSnapshot;
			final RateMetricSnapshot failsSnapshot;
			final RateMetricSnapshot bytesSnapshot;
			final TimingMetricSnapshot actualConcurrencySnapshot;
			final TimingMetricSnapshot durSnapshot;
			final TimingMetricSnapshot latSnapshot;

			if(snapshotsCount == 1) { // single

				final MetricsSnapshot snapshot = snapshots.get(0);
				successSnapshot = snapshot.successSnapshot();
				failsSnapshot = snapshot.failsSnapshot();
				bytesSnapshot = snapshot.byteSnapshot();
				actualConcurrencySnapshot = snapshot.concurrencySnapshot();
				durSnapshot = snapshot.durationSnapshot();
				latSnapshot = snapshot.latencySnapshot();

			} else { // many

				final List<TimingMetricSnapshot> durSnapshots = new ArrayList<>();
				final List<TimingMetricSnapshot> latSnapshots = new ArrayList<>();
				final List<TimingMetricSnapshot> conSnapshots = new ArrayList<>();
				final List<RateMetricSnapshot> succSnapshots = new ArrayList<>();
				final List<RateMetricSnapshot> failSnapshots = new ArrayList<>();
				final List<RateMetricSnapshot> byteSnapshots = new ArrayList<>();
				for(int i = 0; i < snapshotsCount; i ++) {
					final MetricsSnapshot snapshot = snapshots.get(i);
					durSnapshots.add(snapshot.durationSnapshot());
					latSnapshots.add(snapshot.latencySnapshot());
					succSnapshots.add(snapshot.successSnapshot());
					failSnapshots.add(snapshot.failsSnapshot());
					byteSnapshots.add(snapshot.byteSnapshot());
					conSnapshots.add(snapshot.concurrencySnapshot());
				}
				successSnapshot = RateMetricSnapshot.aggregate(succSnapshots);
				failsSnapshot = RateMetricSnapshot.aggregate(failSnapshots);
				bytesSnapshot = RateMetricSnapshot.aggregate(byteSnapshots);
				actualConcurrencySnapshot = TimingMetricSnapshot.aggregate(conSnapshots);
				durSnapshot = TimingMetricSnapshot.aggregate(durSnapshots);
				latSnapshot = TimingMetricSnapshot.aggregate(latSnapshots);

			}

			lastSnapshot = (S) new DistributedMetricsSnapshotImpl(
				durSnapshot, latSnapshot, actualConcurrencySnapshot, failsSnapshot, successSnapshot, bytesSnapshot,
				nodeCountSupplier.getAsInt(), elapsedTimeMillis()
			);
			if(metricsListener != null) {
				metricsListener.notify(lastSnapshot);
			}
			if(thresholdMetricsCtx != null) {
				thresholdMetricsCtx.refreshLastSnapshot();
			}
		}
	}

	@Override
	protected DistributedMetricsContextImpl<S> newThresholdMetricsContext() {
		return new DistributedMetricsContextImpl<>(
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
		return lastSnapshot.byteSnapshot().count();
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
		super.close();
	}
}