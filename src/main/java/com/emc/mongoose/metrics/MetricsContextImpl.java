package com.emc.mongoose.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.UniformReservoir;
import com.codahale.metrics.UniformSnapshot;
import com.emc.mongoose.item.io.IoType;

import com.github.akurilov.commons.system.SizeInBytes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntSupplier;

/**
 Created by kurila on 15.09.15.
 Start timestamp and elapsed time is in milliseconds while other time values are in microseconds.
 */
public class MetricsContextImpl
implements MetricsContext {

	private final Clock clock = new ResumableUserTimeClock();
	private final Histogram reqDuration, respLatency, actualConcurrency;
	private volatile com.codahale.metrics.Snapshot
		reqDurSnapshot, respLatSnapshot, actualConcurrencySnapshot;
	private final LongAdder reqDurationSum, respLatencySum;
	private volatile long lastDurationSum = 0, lastLatencySum = 0;
	private volatile int lastConcurrency;
	private final CustomMeter throughputSuccess, throughputFail, reqBytes;
	private final long ts;
	private volatile long tsStart = -1, prevElapsedTime = 0;

	private final String id;
	private final IoType ioType;
	private final IntSupplier actualConcurrencyGauge;
	private final int concurrency;
	private final int thresholdConcurrency;
	private final SizeInBytes itemDataSize;
	private final boolean stdOutColorFlag;
	private final long outputPeriodMillis;
	private volatile long lastOutputTs = 0;
	private volatile MetricsSnapshot lastSnapshot = null;
	private volatile MetricsListener metricsListener = null;
	private volatile MetricsContext thresholdMetricsCtx = null;
	private volatile boolean thresholdStateExitedFlag = false;

	public MetricsContextImpl(
		final String id, final IoType ioType, final IntSupplier actualConcurrencyGauge,
		final int concurrency, final int thresholdConcurrency, final SizeInBytes itemDataSize,
		final int updateIntervalSec, final boolean stdOutColorFlag
	) {
		this.id = id;
		this.ioType = ioType;
		this.actualConcurrencyGauge = actualConcurrencyGauge;
		this.concurrency = concurrency;
		this.thresholdConcurrency = thresholdConcurrency > 0 ?
			thresholdConcurrency : Integer.MAX_VALUE;
		this.itemDataSize = itemDataSize;

		this.stdOutColorFlag = stdOutColorFlag;
		this.outputPeriodMillis = TimeUnit.SECONDS.toMillis(updateIntervalSec);

		respLatency = new Histogram(new UniformReservoir(DEFAULT_RESERVOIR_SIZE));
		respLatSnapshot = respLatency.getSnapshot();
		respLatencySum = new LongAdder();
		reqDuration = new Histogram(new UniformReservoir(DEFAULT_RESERVOIR_SIZE));
		reqDurSnapshot = reqDuration.getSnapshot();
		actualConcurrency = new Histogram(new UniformReservoir(DEFAULT_RESERVOIR_SIZE));
		actualConcurrencySnapshot = actualConcurrency.getSnapshot();
		reqDurationSum = new LongAdder();
		throughputSuccess = new CustomMeter(clock, updateIntervalSec);
		throughputFail = new CustomMeter(clock, updateIntervalSec);
		reqBytes = new CustomMeter(clock, updateIntervalSec);
		ts = System.nanoTime();
	}
	//
	@Override
	public void start() {
		tsStart = System.currentTimeMillis();
		throughputSuccess.resetStartTime();
		throughputFail.resetStartTime();
		reqBytes.resetStartTime();
	}
	//
	@Override
	public final boolean isStarted() {
		return tsStart > -1;
	}
	//
	@Override
	public final void markElapsedTime(final long millis) {
		prevElapsedTime = millis;
	}
	//
	@Override
	public void close()
	throws IOException {
		prevElapsedTime = System.currentTimeMillis() - tsStart;
		tsStart = -1;
		lastSnapshot = null;
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.close();
			thresholdMetricsCtx = null;
		}
	}
	//
	@Override
	public final void markSucc(final long size, final long duration, final long latency) {
		throughputSuccess.mark();
		reqBytes.mark(size);
		if(latency > 0 && duration > latency) {
			reqDuration.update(duration);
			respLatency.update(latency);
			reqDurationSum.add(duration);
			respLatencySum.add(latency);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markSucc(size, duration, latency);
		}
	}
	//
	@Override
	public final void markPartSucc(final long size, final long duration, final long latency) {
		reqBytes.mark(size);
		if(latency > 0 && duration > latency) {
			reqDuration.update(duration);
			respLatency.update(latency);
			reqDurationSum.add(duration);
			respLatencySum.add(latency);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markPartSucc(size, duration, latency);
		}
	}
	//
	@Override
	public final void markSucc(
		final long count, final long bytes, final long durationValues[], final long latencyValues[]
	) {
		throughputSuccess.mark(count);
		reqBytes.mark(bytes);
		for(final long duration : durationValues) {
			reqDuration.update(duration);
			reqDurationSum.add(duration);
		}
		for(final long latency : latencyValues) {
			respLatency.update(latency);
			respLatencySum.add(latency);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markSucc(count, bytes, durationValues, latencyValues);
		}
	}
	//
	@Override
	public final void markPartSucc(
		final long bytes, final long durationValues[], final long latencyValues[]
	) {
		reqBytes.mark(bytes);
		for(final long duration : durationValues) {
			reqDuration.update(duration);
			reqDurationSum.add(duration);
		}
		for(final long latency : latencyValues) {
			respLatency.update(latency);
			respLatencySum.add(latency);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markPartSucc(bytes, durationValues, latencyValues);
		}
	}
	//
	@Override
	public final void markFail() {
		throughputFail.mark();
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markFail();
		}
	}
	//
	@Override
	public final void markFail(final long count) {
		throughputFail.mark(count);
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markFail(count);
		}
	}
	//
	@Override
	public final String stepId() {
		return id;
	}
	//
	@Override
	public final IoType ioType() {
		return ioType;
	}
	//
	@Override
	public final int nodeCount() {
		return 1;
	}
	//
	@Override
	public final int concurrencyLimit() {
		return concurrency;
	}
	//
	@Override
	public final int concurrencyThreshold() {
		return thresholdConcurrency;
	}

	@Override
	public final int actualConcurrency() {
		return lastConcurrency = actualConcurrencyGauge.getAsInt();
	}
	//
	@Override
	public final SizeInBytes itemDataSize() {
		return itemDataSize;
	}
	//
	@Override
	public final boolean stdOutColorEnabled() {
		return stdOutColorFlag;
	}
	//
	@Override
	public final boolean avgPersistEnabled() {
		return false;
	}
	//
	@Override
	public final boolean sumPersistEnabled() {
		return false;
	}
	//
	@Override
	public final boolean perfDbResultsFileEnabled() {
		return false;
	}
	//
	@Override
	public final long outputPeriodMillis() {
		return outputPeriodMillis;
	}
	//
	@Override
	public final long lastOutputTs() {
		return lastOutputTs;
	}
	
	@Override
	public final void lastOutputTs(final long ts) {
		lastOutputTs = ts;
	}
	//
	@Override
	public final void refreshLastSnapshot() {
		final long currentTimeMillis = System.currentTimeMillis();
		final long currElapsedTime = tsStart > 0 ? currentTimeMillis - tsStart : 0;
		if(currentTimeMillis - lastOutputTs > DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS) {
			if(lastDurationSum != reqDurationSum.sum()) {
				lastDurationSum = reqDurationSum.sum();
				reqDurSnapshot = reqDuration.getSnapshot();
			}
			if(lastLatencySum != respLatencySum.sum()) {
				lastLatencySum = respLatencySum.sum();
				respLatSnapshot = respLatency.getSnapshot();
			};
			actualConcurrency.update(lastConcurrency);
			actualConcurrencySnapshot = actualConcurrency.getSnapshot();
		}
		lastSnapshot =  new MetricsSnapshotImpl(
			throughputSuccess.getCount(), throughputSuccess.getLastRate(),
			throughputFail.getCount(), throughputFail.getLastRate(), reqBytes.getCount(),
			reqBytes.getLastRate(), tsStart, prevElapsedTime + currElapsedTime,
			lastConcurrency, actualConcurrencySnapshot.getMean(),
			lastDurationSum, lastLatencySum, reqDurSnapshot, respLatSnapshot
		);
		if(metricsListener != null) {
			metricsListener.notify(lastSnapshot);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.refreshLastSnapshot();
		}
	}
	//
	@Override
	public final MetricsSnapshot lastSnapshot() {
		if(lastSnapshot == null) {
			refreshLastSnapshot();
		}
		return lastSnapshot;
	}
	//
	@Override
	public final void metricsListener(final MetricsListener metricsListener) {
		this.metricsListener = metricsListener;
	}
	//
	@Override
	public final boolean thresholdStateEntered() {
		return thresholdMetricsCtx != null && thresholdMetricsCtx.isStarted();
	}
	//
	@Override
	public final void enterThresholdState()
	throws IllegalStateException {
		if(thresholdMetricsCtx != null) {
			throw new IllegalStateException("Nested metrics context already exists");
		}
		thresholdMetricsCtx = new MetricsContextImpl(
			id, ioType, actualConcurrencyGauge, concurrency, 0, itemDataSize,
			(int) TimeUnit.MILLISECONDS.toSeconds(outputPeriodMillis), stdOutColorFlag
		);
		thresholdMetricsCtx.start();
	}
	//
	@Override
	public final MetricsContext thresholdMetrics()
	throws IllegalStateException {
		if(thresholdMetricsCtx == null) {
			throw new IllegalStateException("Nested metrics context is not exist");
		}
		return thresholdMetricsCtx;
	}
	//
	@Override
	public final boolean thresholdStateExited() {
		return thresholdStateExitedFlag;
	}
	//
	@Override
	public final void exitThresholdState()
	throws IllegalStateException {
		if(thresholdMetricsCtx == null) {
			throw new IllegalStateException("Threshold state was not entered");
		}
		try {
			thresholdMetricsCtx.close();
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
		thresholdStateExitedFlag = true;
	}
	//
	@Override
	public final int hashCode() {
		return (int) ts;
	}
	//
	@Override
	public final int compareTo(final MetricsContext other) {
		return Long.compare(hashCode(), other.hashCode());
	}
	//
	@Override
	public final String toString() {
		return "MetricsContext(" + ioType.name() + '-' + concurrency + "x1@" + id + ")";
	}
	//
	protected static final class MetricsSnapshotImpl
	implements MetricsSnapshot {
		//
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
		//
		public MetricsSnapshotImpl(
			final long countSucc, final double succRateLast, final long countFail,
			final double failRateLast, final long countByte, final double byteRateLast,
			final long startTimeMillis, final long elapsedTimeMillis, final int actualConcurrencyLast,
			final double actualConcurrencyMean, final long sumDur, final long sumLat,
			final com.codahale.metrics.Snapshot durSnapshot,
			final com.codahale.metrics.Snapshot latSnapshot
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
		//
		@Override
		public final long succCount() {
			return countSucc;
		}
		//
		@Override
		public final double succRateMean() {
			return elapsedTimeMillis == 0 ? 0 : 1000.0 * countSucc / elapsedTimeMillis;
		}
		//
		@Override
		public final double succRateLast() {
			return succRateLast;
		}
		//
		@Override
		public final long failCount() {
			return countFail;
		}
		//
		@Override
		public final double failRateMean() {
			return elapsedTimeMillis == 0 ? 0 : 1000.0 * countFail / elapsedTimeMillis;
		}
		//
		@Override
		public final double failRateLast() {
			return failRateLast;
		}
		//
		@Override
		public final long byteCount() {
			return countByte;
		}
		//
		@Override
		public final double byteRateMean() {
			return elapsedTimeMillis == 0 ? 0 : 1000.0 * countByte / elapsedTimeMillis;
		}
		//
		@Override
		public final double byteRateLast() {
			return byteRateLast;
		}
		//
		@Override
		public final long durationMin() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getMin();
		}
		//
		@Override
		public final long durationLoQ() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return (long) durSnapshot.getValue(0.25);
		}
		//
		@Override
		public final long durationMed() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return (long) durSnapshot.getValue(0.5);
		}
		//
		@Override
		public final long durationHiQ() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return (long) durSnapshot.getValue(0.75);
		}
		//
		@Override
		public final long durationMax() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getMax();
		}
		//
		@Override
		public final long durationSum() {
			return sumDur;
		}
		//
		@Override
		public final double durationMean() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getMean();
		}
		//
		@Override
		public final long[] durationValues() {
			return durValues;
		}
		//
		@Override
		public final long latencyMin() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMin();
		}
		//
		@Override
		public final long latencyLoQ() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return (long) latSnapshot.getValue(0.25);
		}
		//
		@Override
		public final long latencyMed() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return (long) latSnapshot.getValue(0.5);
		}
		//
		@Override
		public final long latencyHiQ() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return (long) latSnapshot.getValue(0.75);
		}//
		@Override
		public final long latencyMax() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMax();
		}
		//
		@Override
		public final long latencySum() {
			return sumLat;
		}
		//
		@Override
		public final double latencyMean() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMean();
		}
		//
		@Override
		public final long[] latencyValues() {
			return latValues;
		}
		//
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
}
