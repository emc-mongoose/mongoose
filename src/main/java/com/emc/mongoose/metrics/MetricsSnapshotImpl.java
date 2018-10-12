package com.emc.mongoose.metrics;

import com.emc.mongoose.metrics.util.RateMetricSnapshot;
import com.emc.mongoose.metrics.util.TimingMetricSnapshot;

public class MetricsSnapshotImpl
	implements MetricsSnapshot {

	private transient TimingMetricSnapshot durSnapshot;
	private transient TimingMetricSnapshot latSnapshot;
	private transient TimingMetricSnapshot actualConcurrencySnapshot;
	private transient RateMetricSnapshot failsSnapshot;
	private transient RateMetricSnapshot successSnapshot;
	private transient RateMetricSnapshot bytesSnapshot;

	public MetricsSnapshotImpl(
		final TimingMetricSnapshot durSnapshot, final TimingMetricSnapshot latSnapshot,
		final TimingMetricSnapshot actualConcurrencySnapshot,
		final RateMetricSnapshot failsSnapshot,
		final RateMetricSnapshot successSnapshot,
		final RateMetricSnapshot bytesSnapshot
	) {
		this.durSnapshot = durSnapshot;
		this.latSnapshot = latSnapshot;
		this.actualConcurrencySnapshot = actualConcurrencySnapshot;
		this.failsSnapshot = failsSnapshot;
		this.successSnapshot = successSnapshot;
		this.bytesSnapshot = bytesSnapshot;
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
}
