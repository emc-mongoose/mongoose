package com.emc.mongoose.metrics.context;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsListener;
import com.emc.mongoose.metrics.DistributedMetricsSnapshotImpl;
import com.emc.mongoose.metrics.MetricsSnapshot;
import com.emc.mongoose.metrics.util.Meter;
import com.emc.mongoose.metrics.util.RateMetricSnapshot;
import com.emc.mongoose.metrics.util.RateMetricSnapshotImpl;
import com.emc.mongoose.metrics.util.TimingMetricSnapshot;
import com.emc.mongoose.metrics.util.TimingMetricSnapshotImpl;
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
	private final List<Meter> metrics;
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
		this.metrics = new ArrayList<>();
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
		final List<TimingMetricSnapshot> durSnapshots = new ArrayList<>();
		final List<TimingMetricSnapshot> latSnapshots = new ArrayList<>();
		final List<TimingMetricSnapshot> conSnapshots = new ArrayList<>();
		final List<RateMetricSnapshot> succSnapshots = new ArrayList<>();
		final List<RateMetricSnapshot> failSnapshots = new ArrayList<>();
		final List<RateMetricSnapshot> byteSnapshots = new ArrayList<>();
		for(final MetricsSnapshot snapshot : snapshots) {
			durSnapshots.add(snapshot.durationSnapshot());
			latSnapshots.add(snapshot.latencySnapshot());
			succSnapshots.add(snapshot.successSnapshot());
			failSnapshots.add(snapshot.failsSnapshot());
			byteSnapshots.add(snapshot.byteSnapshot());
			conSnapshots.add(snapshot.concurrencySnapshot());
		}
		final RateMetricSnapshot successSnapshot = new RateMetricSnapshotImpl(succSnapshots);
		final RateMetricSnapshot failsSnapshot = new RateMetricSnapshotImpl(failSnapshots);
		final RateMetricSnapshot bytesSnapshot = new RateMetricSnapshotImpl(byteSnapshots);
		final TimingMetricSnapshot actualConcurrencySnapshot = new TimingMetricSnapshotImpl(conSnapshots);
		final TimingMetricSnapshot durSnapshot = new TimingMetricSnapshotImpl(durSnapshots);
		final TimingMetricSnapshot latSnapshot = new TimingMetricSnapshotImpl(latSnapshots);
		lastSnapshot = (S) new DistributedMetricsSnapshotImpl(durSnapshot, latSnapshot, actualConcurrencySnapshot,
			successSnapshot, failsSnapshot, bytesSnapshot, nodeCountSupplier.getAsInt()
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
