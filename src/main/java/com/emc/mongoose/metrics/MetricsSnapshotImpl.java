package com.emc.mongoose.metrics;

public class MetricsSnapshotImpl
	implements MetricsSnapshot {

	private final double LO_QUANTILE_VALUE = 0.75;
	private final double HI_QUANTILE_VALUE = 0.75;

	private final long countSucc;
	private final double succRateLast;
	private final long countFail;
	private final double failRateLast;
	private final long countByte;
	private final double byteRateLast;
	private final long durValues[];
	private transient Snapshot durSnapshot = null;
	private final long latValues[];
	private transient Snapshot latSnapshot = null;
	private final long sumDur;
	private final long sumLat;
	private final long startTimeMillis;
	private final long elapsedTimeMillis;
	private final int actualConcurrencyLast;
	private final double actualConcurrencyMean;
	private final int concurrencyLimit;

	public MetricsSnapshotImpl(
		final long countSucc, final double succRateLast, final long countFail, final double failRateLast,
		final long countByte, final double byteRateLast, final long startTimeMillis, final long elapsedTimeMillis,
		final int actualConcurrencyLast, final double actualConcurrencyMean, final int concurrencyLimit,
		final Snapshot durSnapshot, final Snapshot latSnapshot
	) {
		this.countSucc = countSucc;
		this.succRateLast = succRateLast;
		this.countFail = countFail;
		this.failRateLast = failRateLast;
		this.countByte = countByte;
		this.byteRateLast = byteRateLast;
		this.startTimeMillis = startTimeMillis;
		this.elapsedTimeMillis = elapsedTimeMillis;
		this.actualConcurrencyLast = actualConcurrencyLast;
		this.actualConcurrencyMean = actualConcurrencyMean;
		this.concurrencyLimit = concurrencyLimit;
		this.durSnapshot = durSnapshot;
		this.durValues = durSnapshot.values();
		this.latSnapshot = latSnapshot;
		this.latValues = latSnapshot.values();
		this.sumDur = durSnapshot.sum();
		this.sumLat = latSnapshot.sum();
	}

	@Override
	public final int concurrencyLimit() {
		return concurrencyLimit;
	}

	@Override
	public final long succCount() {
		return countSucc;
	}

	@Override
	public final double succRateMean() {
		return elapsedTimeMillis == 0 ? 0 : 1000.0 * countSucc / elapsedTimeMillis;
	}

	@Override
	public final double succRateLast() {
		return succRateLast;
	}

	@Override
	public final long failCount() {
		return countFail;
	}

	@Override
	public final double failRateMean() {
		return elapsedTimeMillis == 0 ? 0 : 1000.0 * countFail / elapsedTimeMillis;
	}

	@Override
	public final double failRateLast() {
		return failRateLast;
	}

	@Override
	public final long byteCount() {
		return countByte;
	}

	@Override
	public final double byteRateMean() {
		return elapsedTimeMillis == 0 ? 0 : 1000.0 * countByte / elapsedTimeMillis;
	}

	@Override
	public final double byteRateLast() {
		return byteRateLast;
	}

	@Override
	public final long durationMin() {
		return new Double(durSnapshot.min()).longValue();
	}

	@Override
	public final long durationLoQ() {
		return new Double(durSnapshot.quantile(HI_QUANTILE_VALUE)).longValue();
	}

	@Override
	public final long durationMed() {
		return new Double(durSnapshot.median()).longValue();
	}

	@Override
	public final long durationHiQ() {
		return new Double(durSnapshot.quantile(LO_QUANTILE_VALUE)).longValue();
	}

	@Override
	public final long durationMax() {
		return new Double(durSnapshot.max()).longValue();
	}

	@Override
	public final long durationSum() {
		return new Double(durSnapshot.sum()).longValue();
	}

	@Override
	public final double durationMean() {
		return new Double(durSnapshot.mean()).longValue();
	}

	@Override
	public final long[] durationValues() {
		return durValues;
	}

	@Override
	public final long latencyMin() {
		return new Double(durSnapshot.min()).longValue();
	}

	@Override
	public final long latencyLoQ() {
		return new Double(latSnapshot.quantile(LO_QUANTILE_VALUE)).longValue();
	}

	@Override
	public final long latencyMed() {
		return new Double(latSnapshot.median()).longValue();
	}

	@Override
	public final long latencyHiQ() {
		return new Double(latSnapshot.quantile(HI_QUANTILE_VALUE)).longValue();
	}

	@Override
	public final long latencyMax() {
		return new Double(latSnapshot.max()).longValue();
	}

	@Override
	public final long latencySum() {
		return new Double(latSnapshot.sum()).longValue();
	}

	@Override
	public final double latencyMean() {
		return new Double(latSnapshot.mean()).longValue();
	}

	@Override
	public final long[] latencyValues() {
		return latValues;
	}

	@Override
	public final long startTimeMillis() {
		return startTimeMillis;
	}

	@Override
	public final long elapsedTimeMillis() {
		return elapsedTimeMillis;
	}

	@Override
	public final int actualConcurrencyLast() {
		return actualConcurrencyLast;
	}

	@Override
	public final double actualConcurrencyMean() {
		return actualConcurrencyMean;
	}
}
