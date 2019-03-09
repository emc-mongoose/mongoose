package com.emc.mongoose.base.metrics.context;

import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.metrics.DistributedMetricsListener;
import com.emc.mongoose.base.metrics.snapshot.AllMetricsSnapshot;
import com.emc.mongoose.base.metrics.snapshot.ConcurrencyMetricSnapshot;
import com.emc.mongoose.base.metrics.snapshot.ConcurrencyMetricSnapshotImpl;
import com.emc.mongoose.base.metrics.snapshot.DistributedAllMetricsSnapshotImpl;
import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshotImpl;
import com.emc.mongoose.base.metrics.snapshot.TimingMetricSnapshot;
import com.emc.mongoose.base.metrics.snapshot.TimingMetricSnapshotImpl;
import com.github.akurilov.commons.system.SizeInBytes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class DistributedMetricsContextImpl<S extends DistributedAllMetricsSnapshotImpl>
				extends MetricsContextBase<S> implements DistributedMetricsContext<S> {

	private final IntSupplier nodeCountSupplier;
	private final Supplier<List<AllMetricsSnapshot>> snapshotsSupplier;
	private final boolean avgPersistFlag;
	private final boolean sumPersistFlag;
	private volatile DistributedMetricsListener metricsListener = null;
	private final List<Double> quantileValues;
	private final List<String> nodeAddrs;

	public DistributedMetricsContextImpl(
					final String id,
					final OpType opType,
					final IntSupplier nodeCountSupplier,
					final int concurrencyLimit,
					final int concurrencyThreshold,
					final SizeInBytes itemDataSize,
					final int updateIntervalSec,
					final boolean stdOutColorFlag,
					final boolean avgPersistFlag,
					final boolean sumPersistFlag,
					final Supplier<List<AllMetricsSnapshot>> snapshotsSupplier,
					final List<Double> quantileValues,
					final List<String> nodeAddrs,
					final String comment) {
		super(
						id,
						opType,
						concurrencyLimit,
						nodeCountSupplier.getAsInt(),
						concurrencyThreshold,
						itemDataSize,
						stdOutColorFlag,
						TimeUnit.SECONDS.toMillis(updateIntervalSec),
						comment);
		this.nodeCountSupplier = nodeCountSupplier;
		this.snapshotsSupplier = snapshotsSupplier;
		this.avgPersistFlag = avgPersistFlag;
		this.sumPersistFlag = sumPersistFlag;
		this.quantileValues = quantileValues;
		this.nodeAddrs = nodeAddrs;
	}

	@Override
	public void markSucc(final long bytes, final long duration, final long latency) {}

	@Override
	public void markPartSucc(final long bytes, final long duration, final long latency) {}

	@Override
	public void markSucc(
					final long count,
					final long bytes,
					final long[] durationValues,
					final long[] latencyValues) {}

	@Override
	public void markPartSucc(
					final long bytes, final long[] durationValues, final long[] latencyValues) {}

	@Override
	public void markFail() {}

	@Override
	public void markFail(final long count) {}

	@Override
	public List<String> nodeAddrs() {
		return nodeAddrs;
	}

	@Override
	public int nodeCount() {
		return nodeCountSupplier.getAsInt();
	}

	@Override
	public List<Double> quantileValues() {
		return quantileValues;
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
	@SuppressWarnings("unchecked")
	public void refreshLastSnapshot() {

		final var snapshots = snapshotsSupplier.get();
		final var snapshotsCount = snapshots.size();

		if (snapshotsCount > 0) { // do nothing otherwise

			final RateMetricSnapshot successSnapshot;
			final RateMetricSnapshot failsSnapshot;
			final RateMetricSnapshot bytesSnapshot;
			final ConcurrencyMetricSnapshot actualConcurrencySnapshot;
			final TimingMetricSnapshot durSnapshot;
			final TimingMetricSnapshot latSnapshot;

			if (snapshotsCount == 1) { // single

				final var snapshot = snapshots.get(0);
				successSnapshot = snapshot.successSnapshot();
				failsSnapshot = snapshot.failsSnapshot();
				bytesSnapshot = snapshot.byteSnapshot();
				actualConcurrencySnapshot = snapshot.concurrencySnapshot();
				durSnapshot = snapshot.durationSnapshot();
				latSnapshot = snapshot.latencySnapshot();

			} else { // many

				final List<TimingMetricSnapshot> durSnapshots = new ArrayList<>();
				final List<TimingMetricSnapshot> latSnapshots = new ArrayList<>();
				final List<ConcurrencyMetricSnapshot> conSnapshots = new ArrayList<>();
				final List<RateMetricSnapshot> succSnapshots = new ArrayList<>();
				final List<RateMetricSnapshot> failSnapshots = new ArrayList<>();
				final List<RateMetricSnapshot> byteSnapshots = new ArrayList<>();
				for (var i = 0; i < snapshotsCount; i++) {
					final var snapshot = snapshots.get(i);
					durSnapshots.add(snapshot.durationSnapshot());
					latSnapshots.add(snapshot.latencySnapshot());
					succSnapshots.add(snapshot.successSnapshot());
					failSnapshots.add(snapshot.failsSnapshot());
					byteSnapshots.add(snapshot.byteSnapshot());
					conSnapshots.add(snapshot.concurrencySnapshot());
				}
				successSnapshot = RateMetricSnapshotImpl.aggregate(succSnapshots);
				failsSnapshot = RateMetricSnapshotImpl.aggregate(failSnapshots);
				bytesSnapshot = RateMetricSnapshotImpl.aggregate(byteSnapshots);
				actualConcurrencySnapshot = ConcurrencyMetricSnapshotImpl.aggregate(conSnapshots);
				durSnapshot = TimingMetricSnapshotImpl.aggregate(durSnapshots);
				latSnapshot = TimingMetricSnapshotImpl.aggregate(latSnapshots);
			}

			lastSnapshot = (S) new DistributedAllMetricsSnapshotImpl(
							durSnapshot,
							latSnapshot,
							actualConcurrencySnapshot,
							failsSnapshot,
							successSnapshot,
							bytesSnapshot,
							nodeCountSupplier.getAsInt(),
							elapsedTimeMillis());
			if (metricsListener != null) {
				metricsListener.notify(lastSnapshot);
			}
			if (thresholdMetricsCtx != null) {
				thresholdMetricsCtx.refreshLastSnapshot();
			}
		}
	}

	@Override
	protected DistributedMetricsContextImpl<S> newThresholdMetricsContext() {
		return new DistributedContextBuilderImpl()
						.id(id)
						.opType(opType)
						.nodeCountSupplier(nodeCountSupplier)
						.concurrencyLimit(concurrencyLimit)
						.concurrencyThreshold(concurrencyThreshold)
						.itemDataSize(itemDataSize)
						.outputPeriodSec((int) TimeUnit.MILLISECONDS.toSeconds(outputPeriodMillis))
						.stdOutColorFlag(stdOutColorFlag)
						.avgPersistFlag(avgPersistFlag)
						.sumPersistFlag(sumPersistFlag)
						.snapshotsSupplier(snapshotsSupplier)
						.quantileValues(quantileValues)
						.nodeAddrs(nodeAddrs)
						.build();
	}

	@Override
	public final boolean equals(final Object other) {
		if (null == other) {
			return false;
		}
		if (other instanceof MetricsContext) {
			return 0 == compareTo((MetricsContext) other);
		} else {
			return false;
		}
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName()
						+ "("
						+ opType.name()
						+ '-'
						+ concurrencyLimit
						+ "x"
						+ nodeCount()
						+ "@"
						+ id
						+ ")";
	}

	@Override
	public final void close() {
		super.close();
	}

	public static DistributedContextBuilder builder() {
		return new DistributedContextBuilderImpl();
	}

	private static class DistributedContextBuilderImpl implements DistributedContextBuilder {

		private IntSupplier nodeCountSupplier;
		private Supplier<List<AllMetricsSnapshot>> snapshotsSupplier;
		private boolean avgPersistFlag;
		private boolean sumPersistFlag;
		private List<Double> quantileValues;
		private List<String> nodeAddrs;
		private String id;
		private OpType opType;
		private int concurrencyLimit;
		private int concurrencyThreshold;
		private SizeInBytes itemDataSize;
		private boolean stdOutColorFlag;
		private int outputPeriodSec;
		private IntSupplier actualConcurrencyGauge = () -> 1; // TODO: How to correctly define for distributed mode
		private String comment;

		public DistributedMetricsContextImpl build() {
			return new DistributedMetricsContextImpl(
							id,
							opType,
							nodeCountSupplier,
							concurrencyLimit,
							concurrencyThreshold,
							itemDataSize,
							outputPeriodSec,
							stdOutColorFlag,
							avgPersistFlag,
							sumPersistFlag,
							snapshotsSupplier,
							quantileValues,
							nodeAddrs,
							comment);
		}

		public DistributedContextBuilder id(final String id) {
			this.id = id;
			return this;
		}

		public DistributedContextBuilder comment(final String comment) {
			this.comment = comment;
			return this;
		}

		public DistributedContextBuilder opType(final OpType opType) {
			this.opType = opType;
			return this;
		}

		public DistributedContextBuilder concurrencyLimit(final int concurrencyLimit) {
			this.concurrencyLimit = concurrencyLimit;
			return this;
		}

		public DistributedContextBuilder concurrencyThreshold(final int concurrencyThreshold) {
			this.concurrencyThreshold = concurrencyThreshold;
			return this;
		}

		public DistributedContextBuilder itemDataSize(final SizeInBytes itemDataSize) {
			this.itemDataSize = itemDataSize;
			return this;
		}

		public DistributedContextBuilder stdOutColorFlag(final boolean stdOutColorFlag) {
			this.stdOutColorFlag = stdOutColorFlag;
			return this;
		}

		public DistributedContextBuilder outputPeriodSec(final int outputPeriodSec) {
			this.outputPeriodSec = outputPeriodSec;
			return this;
		}

		public DistributedContextBuilder actualConcurrencyGauge(
						final IntSupplier actualConcurrencyGauge) {
			this.actualConcurrencyGauge = actualConcurrencyGauge;
			return this;
		}

		public DistributedContextBuilder avgPersistFlag(final boolean avgPersistFlag) {
			this.avgPersistFlag = avgPersistFlag;
			return this;
		}

		public DistributedContextBuilder sumPersistFlag(final boolean sumPersistFlag) {
			this.sumPersistFlag = sumPersistFlag;
			return this;
		}

		public DistributedContextBuilder quantileValues(final List<Double> quantileValues) {
			this.quantileValues = quantileValues;
			return this;
		}

		public DistributedContextBuilder nodeAddrs(final List<String> nodeAddrs) {
			this.nodeAddrs = nodeAddrs;
			return this;
		}

		public DistributedContextBuilder nodeCountSupplier(final IntSupplier nodeCountSupplier) {
			this.nodeCountSupplier = nodeCountSupplier;
			return this;
		}

		public DistributedContextBuilder snapshotsSupplier(
						final Supplier<List<AllMetricsSnapshot>> snapshotsSupplier) {
			this.snapshotsSupplier = snapshotsSupplier;
			return this;
		}
	}
}
