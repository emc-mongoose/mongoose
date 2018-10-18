package com.emc.mongoose.metrics;

import com.emc.mongoose.metrics.util.RateMetricSnapshot;
import com.emc.mongoose.metrics.util.TimingMetricSnapshot;

public class MetricsSnapshotImpl
	implements MetricsSnapshot {

	private final TimingMetricSnapshot durSnapshot;
	private final TimingMetricSnapshot latSnapshot;
	private final TimingMetricSnapshot actualConcurrencySnapshot;
	private final RateMetricSnapshot failsSnapshot;
	private final RateMetricSnapshot successSnapshot;
	private final RateMetricSnapshot bytesSnapshot;
	protected final long elapsedTimeMillis;

	public MetricsSnapshotImpl(
		final TimingMetricSnapshot durSnapshot,
		final TimingMetricSnapshot latSnapshot,
		final TimingMetricSnapshot actualConcurrencySnapshot,
		final RateMetricSnapshot failsSnapshot,
		final RateMetricSnapshot successSnapshot,
		final RateMetricSnapshot bytesSnapshot,
		final long elapsedTimeMillis
	) {
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
	public TimingMetricSnapshot concurrencySnapshot() {
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
