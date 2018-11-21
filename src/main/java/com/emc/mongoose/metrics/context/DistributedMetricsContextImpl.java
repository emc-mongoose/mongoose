package com.emc.mongoose.metrics.context;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsListener;
import com.emc.mongoose.metrics.snapshot.AllMetricsSnapshot;
import com.emc.mongoose.metrics.snapshot.ConcurrencyMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.ConcurrencyMetricSnapshotImpl;
import com.emc.mongoose.metrics.snapshot.DistributedAllMetricsSnapshotImpl;
import com.emc.mongoose.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.RateMetricSnapshotImpl;
import com.emc.mongoose.metrics.snapshot.TimingMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.TimingMetricSnapshotImpl;
import com.github.akurilov.commons.system.SizeInBytes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class DistributedMetricsContextImpl<S extends DistributedAllMetricsSnapshotImpl>
	extends MetricsContextBase<S>
	implements DistributedMetricsContext<S> {

	private final IntSupplier nodeCountSupplier;
	private final Supplier<List<AllMetricsSnapshot>> snapshotsSupplier;
	private final boolean avgPersistFlag;
	private final boolean sumPersistFlag;
	private final boolean perfDbResultsFileFlag;
	private volatile DistributedMetricsListener metricsListener = null;
	private final List<Double> quantileValues;
	private final List<String> nodeAddrs;

	public DistributedMetricsContextImpl(
		final String id, final OpType opType, final IntSupplier nodeCountSupplier, final int concurrencyLimit,
		final int concurrencyThreshold, final SizeInBytes itemDataSize, final int updateIntervalSec,
		final boolean stdOutColorFlag, final boolean avgPersistFlag, final boolean sumPersistFlag,
		final boolean perfDbResultsFileFlag, final Supplier<List<AllMetricsSnapshot>> snapshotsSupplier,
		final List<Double> quantileValues, final List<String> nodeAddrs
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
		this.quantileValues = quantileValues;
		this.nodeAddrs = nodeAddrs;
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
	public boolean perfDbResultsFileEnabled() {
		return perfDbResultsFileFlag;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void refreshLastSnapshot() {
		final List<AllMetricsSnapshot> snapshots = snapshotsSupplier.get();
		final int snapshotsCount = snapshots.size();
		if(snapshotsCount > 0) { // do nothing otherwise
			final RateMetricSnapshot successSnapshot;
			final RateMetricSnapshot failsSnapshot;
			final RateMetricSnapshot bytesSnapshot;
			final ConcurrencyMetricSnapshot actualConcurrencySnapshot;
			final TimingMetricSnapshot durSnapshot;
			final TimingMetricSnapshot latSnapshot;
			if(snapshotsCount == 1) { // single
				final AllMetricsSnapshot snapshot = snapshots.get(0);
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
				for(int i = 0; i < snapshotsCount; i++) {
					final AllMetricsSnapshot snapshot = snapshots.get(i);
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
		return new DistributedMetricsContextImpl.Builder()
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

	public static class Builder {

		private IntSupplier nodeCountSupplier;
		private Supplier<List<AllMetricsSnapshot>> snapshotsSupplier;
		private boolean avgPersistFlag;
		private boolean sumPersistFlag;
		private boolean perfDbResultsFileFlag;
		private List<Double> quantileValues;
		private List<String> nodeAddrs;
		private String id;
		private OpType opType;
		private int concurrencyLimit;
		private int concurrencyThreshold;
		private SizeInBytes itemDataSize;
		private boolean stdOutColorFlag;
		private int outputPeriodSec;

		public DistributedMetricsContextImpl build() {
			Arrays.asList(this.getClass().getDeclaredFields()).forEach(field -> {
				try {
					if(field.get(this) == null) {
						throw new AssertionError("Field " + field.getName() + " is null");
					}
				} catch(IllegalAccessException e) {
					e.printStackTrace();
				}
			});
			return new DistributedMetricsContextImpl(id, opType, nodeCountSupplier, concurrencyLimit,
				concurrencyThreshold, itemDataSize, outputPeriodSec, stdOutColorFlag, avgPersistFlag, sumPersistFlag,
				perfDbResultsFileFlag, snapshotsSupplier, quantileValues, nodeAddrs
			);
		}

		public Builder id(final String id) {
			this.id = id;
			return this;
		}

		public Builder opType(final OpType opType) {
			this.opType = opType;
			return this;
		}

		public Builder concurrencyLimit(final int concurrencyLimit) {
			this.concurrencyLimit = concurrencyLimit;
			return this;
		}

		public Builder concurrencyThreshold(final int concurrencyThreshold) {
			this.concurrencyThreshold = concurrencyThreshold;
			return this;
		}

		public Builder itemDataSize(final SizeInBytes itemDataSize) {
			this.itemDataSize = itemDataSize;
			return this;
		}

		public Builder stdOutColorFlag(final boolean stdOutColorFlag) {
			this.stdOutColorFlag = stdOutColorFlag;
			return this;
		}

		public Builder outputPeriodSec(final int outputPeriodSec) {
			this.outputPeriodSec = outputPeriodSec;
			return this;
		}

		public Builder avgPersistFlag(final boolean avgPersistFlag) {
			this.avgPersistFlag = avgPersistFlag;
			return this;
		}

		public Builder sumPersistFlag(final boolean sumPersistFlag) {
			this.sumPersistFlag = sumPersistFlag;
			return this;
		}

		public Builder perfDbResultsFileFlag(final boolean perfDbResultsFileFlag) {
			this.perfDbResultsFileFlag = perfDbResultsFileFlag;
			return this;
		}

		public Builder quantileValues(final List<Double> quantileValues) {
			this.quantileValues = quantileValues;
			return this;
		}

		public Builder nodeAddrs(final List<String> nodeAddrs) {
			this.nodeAddrs = nodeAddrs;
			return this;
		}

		public Builder nodeCountSupplier(final IntSupplier nodeCountSupplier) {
			this.nodeCountSupplier = nodeCountSupplier;
			return this;
		}

		public Builder snapshotsSupplier(final Supplier<List<AllMetricsSnapshot>> snapshotsSupplier) {
			this.snapshotsSupplier = snapshotsSupplier;
			return this;
		}
	}
}
