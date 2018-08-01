package com.emc.mongoose.metrics;

import com.codahale.metrics.UniformSnapshot;

public final class MetricsSnapshotImpl
implements MetricsSnapshot {

	private final long countSucc;
	private final double succRateLast;
	private final long countFail;
	private final double failRateLast;
	private final long countByte;
	private final double byteRateLast;
	private final long durValues[];
	private transient com.codahale.metrics.Snapshot durSnapshot = null;
	private final long latValues[];
	private transient com.codahale.metrics.Snapshot latSnapshot = null;
	private final long sumDur;
	private final long sumLat;
	private final long startTimeMillis;
	private final long elapsedTimeMillis;
	private final int actualConcurrencyLast;
	private final double actualConcurrencyMean;

	public MetricsSnapshotImpl(
		final long countSucc, final double succRateLast, final long countFail, final double failRateLast,
		final long countByte, final double byteRateLast, final long startTimeMillis, final long elapsedTimeMillis,
		final int actualConcurrencyLast, final double actualConcurrencyMean, final long sumDur, final long sumLat,
		final com.codahale.metrics.Snapshot durSnapshot, final com.codahale.metrics.Snapshot latSnapshot
	) {
		this.countSucc = countSucc;
		this.succRateLast = succRateLast;
		this.countFail = countFail;
		this.failRateLast = failRateLast;
		this.countByte = countByte;
		this.byteRateLast = byteRateLast;
		this.sumDur = sumDur;
		this.sumLat = sumLat;
		this.startTimeMillis = startTimeMillis;
		this.elapsedTimeMillis = elapsedTimeMillis;
		this.actualConcurrencyLast = actualConcurrencyLast;
		this.actualConcurrencyMean = actualConcurrencyMean;
		this.durSnapshot = durSnapshot;
		this.durValues = durSnapshot.getValues();
		this.latSnapshot = latSnapshot;
		this.latValues = latSnapshot.getValues();
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
		if(durSnapshot == null) {
			durSnapshot = new UniformSnapshot(durValues);
		}
		return durSnapshot.getMin();
	}

	@Override
	public final long durationLoQ() {
		if(durSnapshot == null) {
			durSnapshot = new UniformSnapshot(durValues);
		}
		return (long) durSnapshot.getValue(0.25);
	}

	@Override
	public final long durationMed() {
		if(durSnapshot == null) {
			durSnapshot = new UniformSnapshot(durValues);
		}
		return (long) durSnapshot.getValue(0.5);
	}

	@Override
	public final long durationHiQ() {
		if(durSnapshot == null) {
			durSnapshot = new UniformSnapshot(durValues);
		}
		return (long) durSnapshot.getValue(0.75);
	}

	@Override
	public final long durationMax() {
		if(durSnapshot == null) {
			durSnapshot = new UniformSnapshot(durValues);
		}
		return durSnapshot.getMax();
	}

	@Override
	public final long durationSum() {
		return sumDur;
	}

	@Override
	public final double durationMean() {
		if(durSnapshot == null) {
			durSnapshot = new UniformSnapshot(durValues);
		}
		return durSnapshot.getMean();
	}

	@Override
	public final long[] durationValues() {
		return durValues;
	}

	@Override
	public final long latencyMin() {
		if(latSnapshot == null) {
			latSnapshot = new UniformSnapshot(latValues);
		}
		return latSnapshot.getMin();
	}

	@Override
	public final long latencyLoQ() {
		if(latSnapshot == null) {
			latSnapshot = new UniformSnapshot(latValues);
		}
		return (long) latSnapshot.getValue(0.25);
	}

	@Override
	public final long latencyMed() {
		if(latSnapshot == null) {
			latSnapshot = new UniformSnapshot(latValues);
		}
		return (long) latSnapshot.getValue(0.5);
	}

	@Override
	public final long latencyHiQ() {
		if(latSnapshot == null) {
			latSnapshot = new UniformSnapshot(latValues);
		}
		return (long) latSnapshot.getValue(0.75);
	}

	@Override
	public final long latencyMax() {
		if(latSnapshot == null) {
			latSnapshot = new UniformSnapshot(latValues);
		}
		return latSnapshot.getMax();
	}

	@Override
	public final long latencySum() {
		return sumLat;
	}

	@Override
	public final double latencyMean() {
		if(latSnapshot == null) {
			latSnapshot = new UniformSnapshot(latValues);
		}
		return latSnapshot.getMean();
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
