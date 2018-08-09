package com.emc.mongoose.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.emc.mongoose.item.op.OpType;
import com.github.akurilov.commons.system.SizeInBytes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;

/**
 Created by kurila on 15.09.15.
 Start timestamp and elapsed time is in milliseconds while other time values are in microseconds.
 */
public class MetricsContextImpl
	implements MetricsContext {

	private final Clock clock = new ResumableUserTimeClock();
	private final Histogram reqDuration, respLatency, actualConcurrency;
	private volatile com.codahale.metrics.Snapshot reqDurSnapshot, respLatSnapshot, actualConcurrencySnapshot;
	private final LongAdder reqDurationSum, respLatencySum;
	private volatile long lastDurationSum = 0, lastLatencySum = 0;
	private final CustomMeter throughputSuccess, throughputFail, reqBytes;
	private final long ts;
	private volatile long tsStart = - 1, prevElapsedTime = 0;
	private final String id;
	private final OpType opType;
	private final IntSupplier actualConcurrencyGauge;
	private final int concurrencyLimit;
	private final int thresholdConcurrency;
	private final SizeInBytes itemDataSize;
	private final boolean stdOutColorFlag;
	private final long outputPeriodMillis;
	private volatile long lastOutputTs = 0;
	private volatile MetricsSnapshot lastSnapshot = null;
	private volatile MetricsListener metricsListener = null;
	private volatile MetricsContext thresholdMetricsCtx = null;
	private volatile boolean thresholdStateExitedFlag = false;
	private final Lock timingLock = new ReentrantLock();

	public MetricsContextImpl(
		final String id, final OpType opType, final IntSupplier actualConcurrencyGauge, final int concurrencyLimit,
		final int thresholdConcurrency, final SizeInBytes itemDataSize, final int updateIntervalSec,
		final boolean stdOutColorFlag
	) {
		this.id = id;
		this.opType = opType;
		this.actualConcurrencyGauge = actualConcurrencyGauge;
		this.concurrencyLimit = concurrencyLimit;
		this.thresholdConcurrency = thresholdConcurrency > 0 ? thresholdConcurrency : Integer.MAX_VALUE;
		this.itemDataSize = itemDataSize;
		this.stdOutColorFlag = stdOutColorFlag;
		this.outputPeriodMillis = TimeUnit.SECONDS.toMillis(updateIntervalSec);
		respLatency = new Histogram(new ConcurrentSlidingWindowReservoir(DEFAULT_RESERVOIR_SIZE));
		respLatSnapshot = respLatency.getSnapshot();
		respLatencySum = new LongAdder();
		reqDuration = new Histogram(new ConcurrentSlidingWindowReservoir(DEFAULT_RESERVOIR_SIZE));
		reqDurSnapshot = reqDuration.getSnapshot();
		actualConcurrency = new Histogram(new ConcurrentSlidingWindowReservoir(DEFAULT_RESERVOIR_SIZE));
		actualConcurrencySnapshot = actualConcurrency.getSnapshot();
		reqDurationSum = new LongAdder();
		throughputSuccess = new CustomMeter(clock, updateIntervalSec);
		throughputFail = new CustomMeter(clock, updateIntervalSec);
		reqBytes = new CustomMeter(clock, updateIntervalSec);
		ts = System.nanoTime();
	}

	@Override
	public void start() {
		tsStart = System.currentTimeMillis();
		throughputSuccess.resetStartTime();
		throughputFail.resetStartTime();
		reqBytes.resetStartTime();
	}

	@Override
	public final boolean isStarted() {
		return tsStart > - 1;
	}

	@Override
	public final void markElapsedTime(final long millis) {
		prevElapsedTime = millis;
	}

	@Override
	public void close()
	throws IOException {
		prevElapsedTime = System.currentTimeMillis() - tsStart;
		tsStart = - 1;
		lastSnapshot = null;
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.close();
			thresholdMetricsCtx = null;
		}
	}

	@Override
	public final void markSucc(final long bytes, final long duration, final long latency) {
		throughputSuccess.mark();
		reqBytes.mark(bytes);
		if(latency > 0 && duration > latency) {
			timingLock.lock();
			try {
				reqDuration.update(duration);
				respLatency.update(latency);
			} finally {
				timingLock.unlock();
			}
			reqDurationSum.add(duration);
			respLatencySum.add(latency);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markSucc(bytes, duration, latency);
		}
	}

	@Override
	public final void markPartSucc(final long bytes, final long duration, final long latency) {
		reqBytes.mark(bytes);
		if(latency > 0 && duration > latency) {
			timingLock.lock();
			try {
				reqDuration.update(duration);
				respLatency.update(latency);
			} finally {
				timingLock.unlock();
			}
			reqDurationSum.add(duration);
			respLatencySum.add(latency);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markPartSucc(bytes, duration, latency);
		}
	}

	@Override
	public final void markSucc(
		final long count, final long bytes, final long durationValues[], final long latencyValues[]
	) {
		throughputSuccess.mark(count);
		reqBytes.mark(bytes);
		final int timingsLen = Math.min(durationValues.length, latencyValues.length);
		long duration, latency;
		for(int i = 0; i < timingsLen; i++) {
			duration = durationValues[i];
			latency = latencyValues[i];
			if(latency > 0 && duration > latency) {
				timingLock.lock();
				try {
					reqDuration.update(duration);
					respLatency.update(latency);
				} finally {
					timingLock.unlock();
				}
				reqDurationSum.add(duration);
				respLatencySum.add(latency);
			}
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markSucc(count, bytes, durationValues, latencyValues);
		}
	}

	@Override
	public final void markPartSucc(
		final long bytes, final long durationValues[], final long latencyValues[]
	) {
		reqBytes.mark(bytes);
		final int timingsLen = Math.min(durationValues.length, latencyValues.length);
		long duration, latency;
		for(int i = 0; i < timingsLen; i++) {
			duration = durationValues[i];
			latency = latencyValues[i];
			if(latency > 0 && duration > latency) {
				timingLock.lock();
				try {
					reqDuration.update(duration);
					respLatency.update(latency);
				} finally {
					timingLock.unlock();
				}
				reqDurationSum.add(duration);
				respLatencySum.add(latency);
			}
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markPartSucc(bytes, durationValues, latencyValues);
		}
	}

	@Override
	public final void markFail() {
		throughputFail.mark();
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markFail();
		}
	}

	@Override
	public final void markFail(final long count) {
		throughputFail.mark(count);
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.markFail(count);
		}
	}

	@Override
	public final String stepId() {
		return id;
	}

	@Override
	public final OpType opType() {
		return opType;
	}

	@Override
	public final int concurrencyLimit() {
		return concurrencyLimit;
	}

	@Override
	public final int concurrencyThreshold() {
		return thresholdConcurrency;
	}

	@Override
	public final SizeInBytes itemDataSize() {
		return itemDataSize;
	}

	@Override
	public final boolean stdOutColorEnabled() {
		return stdOutColorFlag;
	}

	@Override
	public final boolean avgPersistEnabled() {
		return false;
	}

	@Override
	public final boolean sumPersistEnabled() {
		return false;
	}

	@Override
	public final boolean perfDbResultsFileEnabled() {
		return false;
	}

	@Override
	public final long outputPeriodMillis() {
		return outputPeriodMillis;
	}

	@Override
	public final long lastOutputTs() {
		return lastOutputTs;
	}

	@Override
	public final void lastOutputTs(final long ts) {
		lastOutputTs = ts;
	}

	@Override
	public final void refreshLastSnapshot() {
		final long currentTimeMillis = System.currentTimeMillis();
		final long currElapsedTime = tsStart > 0 ? currentTimeMillis - tsStart : 0;
		if(currentTimeMillis - lastOutputTs > DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS) {
			if(lastDurationSum != reqDurationSum.sum() || lastLatencySum != respLatencySum.sum()) {
				if(timingLock.tryLock()) {
					try {
						reqDurSnapshot = reqDuration.getSnapshot();
						respLatSnapshot = respLatency.getSnapshot();
					} finally {
						timingLock.unlock();
					}
				}
				lastLatencySum = respLatencySum.sum();
				lastDurationSum = reqDurationSum.sum();
			}
			actualConcurrency.update(actualConcurrencyGauge.getAsInt());
			actualConcurrencySnapshot = actualConcurrency.getSnapshot();
		}
		lastSnapshot = new MetricsSnapshotImpl(throughputSuccess.getCount(), throughputSuccess.
			getLastRate(), throughputFail.getCount(), throughputFail.getLastRate(), reqBytes.getCount(),
			reqBytes.getLastRate(), tsStart, prevElapsedTime + currElapsedTime, actualConcurrencyGauge.getAsInt(),
			actualConcurrencySnapshot.getMean(), concurrencyLimit, lastDurationSum, lastLatencySum, reqDurSnapshot,
			respLatSnapshot
		);
		if(metricsListener != null) {
			metricsListener.notify(lastSnapshot);
		}
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.refreshLastSnapshot();
		}
	}

	@Override
	public final MetricsSnapshot lastSnapshot() {
		if(lastSnapshot == null) {
			refreshLastSnapshot();
		}
		return lastSnapshot;
	}

	@Override
	public final void metricsListener(final MetricsListener metricsListener) {
		this.metricsListener = metricsListener;
	}

	@Override
	public final long transferSizeSum() {
		return reqBytes.getCount();
	}

	@Override
	public final boolean thresholdStateEntered() {
		return thresholdMetricsCtx != null && thresholdMetricsCtx.isStarted();
	}

	@Override
	public final void enterThresholdState()
	throws IllegalStateException {
		if(thresholdMetricsCtx != null) {
			throw new IllegalStateException("Nested metrics context already exists");
		}
		thresholdMetricsCtx =
			new MetricsContextImpl(id, opType, actualConcurrencyGauge, concurrencyLimit, 0, itemDataSize,
				(int) TimeUnit.MILLISECONDS.toSeconds(outputPeriodMillis), stdOutColorFlag
			);
		thresholdMetricsCtx.start();
	}

	@Override
	public final MetricsContext thresholdMetrics()
	throws IllegalStateException {
		if(thresholdMetricsCtx == null) {
			throw new IllegalStateException("Nested metrics context is not exist");
		}
		return thresholdMetricsCtx;
	}

	@Override
	public final boolean thresholdStateExited() {
		return thresholdStateExitedFlag;
	}

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

	@Override
	public final int hashCode() {
		return (int) ts;
	}

	@Override
	public final int compareTo(final MetricsContext other) {
		return Long.compare(hashCode(), other.hashCode());
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
		return "MetricsContext(" + opType.name() + '-' + concurrencyLimit + "x1@" + id + ")";
	}
}
