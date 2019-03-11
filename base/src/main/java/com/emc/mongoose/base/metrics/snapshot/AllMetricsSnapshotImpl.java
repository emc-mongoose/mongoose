package com.emc.mongoose.base.metrics.snapshot;

public class AllMetricsSnapshotImpl implements AllMetricsSnapshot {

	private final TimingMetricSnapshot durSnapshot;
	private final TimingMetricSnapshot latSnapshot;
	private final ConcurrencyMetricSnapshot actualConcurrencySnapshot;
	private final RateMetricSnapshot failsSnapshot;
	private final RateMetricSnapshot successSnapshot;
	private final RateMetricSnapshot bytesSnapshot;
	protected final long elapsedTimeMillis;

	public AllMetricsSnapshotImpl(
					final TimingMetricSnapshot durSnapshot,
					final TimingMetricSnapshot latSnapshot,
					final ConcurrencyMetricSnapshot actualConcurrencySnapshot,
					final RateMetricSnapshot failsSnapshot,
					final RateMetricSnapshot successSnapshot,
					final RateMetricSnapshot bytesSnapshot,
					final long elapsedTimeMillis) {
		this.durSnapshot = durSnapshot;
		this.latSnapshot = latSnapshot;
		this.actualConcurrencySnapshot = actualConcurrencySnapshot;
		this.failsSnapshot = failsSnapshot;
		this.successSnapshot = successSnapshot;
		this.bytesSnapshot = bytesSnapshot;
		this.elapsedTimeMillis = elapsedTimeMillis;
	}

	@Override
	public TimingMetricSnapshot durationSnapshot() {
		return durSnapshot;
	}

	@Override
	public TimingMetricSnapshot latencySnapshot() {
		return latSnapshot;
	}

	@Override
	public ConcurrencyMetricSnapshot concurrencySnapshot() {
		return actualConcurrencySnapshot;
	}

	@Override
	public RateMetricSnapshot byteSnapshot() {
		return bytesSnapshot;
	}

	@Override
	public RateMetricSnapshot successSnapshot() {
		return successSnapshot;
	}

	@Override
	public RateMetricSnapshot failsSnapshot() {
		return failsSnapshot;
	}

	@Override
	public long elapsedTimeMillis() {
		return elapsedTimeMillis;
	}
}
