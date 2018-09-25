package com.emc.mongoose.metrics;

import com.emc.mongoose.item.op.OpType;
import com.github.akurilov.commons.system.SizeInBytes;
import io.prometheus.client.Summary;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 Created by kurila on 15.09.15.
 Start timestamp and elapsed time is in milliseconds while other time values are in microseconds.
 */
public class MetricsContextImpl<S extends MetricsSnapshotImpl>
	extends MetricsContextBase<S>
	implements MetricsContext<S> {

	private final Clock clock = Clock.systemDefaultZone();
	private final Summary reqDuration, respLatency, actualConcurrency;
	private final LongAdder reqDurationSum, respLatencySum;
	private final CustomMeter throughputSuccess, throughputFail, reqBytes;
	private final IntSupplier actualConcurrencyGauge;
	private final Lock timingLock = new ReentrantLock();
	private volatile long prevElapsedTime = 0;
	private volatile long lastDurationSum = 0;
	private volatile long lastLatencySum = 0;
	private volatile Snapshot reqDurSnapshot, respLatSnapshot, actualConcurrencySnapshot;

	public MetricsContextImpl(
		final String id, final OpType opType, final IntSupplier actualConcurrencyGauge, final int concurrencyLimit,
		final int concurrencyThreshold, final SizeInBytes itemDataSize, final int updateIntervalSec,
		final boolean stdOutColorFlag
	) {
		super(
			id, opType, concurrencyLimit, concurrencyThreshold, itemDataSize, stdOutColorFlag,
			TimeUnit.SECONDS.toMillis(updateIntervalSec)
		);
		this.actualConcurrencyGauge = actualConcurrencyGauge;

		final BiFunction<String,String,Summary> buildSummary = (name,help) -> Summary
			.build()
			.quantile(Double.MIN_VALUE, 0.0)  // min
			.quantile(1.0, 0.0)  // max
			.quantile(0.25,0.01) //loQ
			.quantile(0.75,0.01) //hiQ
			.name(name)
			.help(help)
			.register();

		respLatency = buildSummary.apply("respLatency","Latency of response");
		respLatSnapshot = new Snapshot(respLatency);
		respLatencySum = new LongAdder();
		reqDuration = buildSummary.apply("reqDuration","duration of request");
		reqDurSnapshot = new Snapshot(reqDuration);
		actualConcurrency = buildSummary.apply("actualConcurrency","actually value of concurrency");
		actualConcurrencySnapshot = new Snapshot(actualConcurrency);
		reqDurationSum = new LongAdder();
		throughputSuccess = new CustomMeter(clock, updateIntervalSec);
		throughputFail = new CustomMeter(clock, updateIntervalSec);
		reqBytes = new CustomMeter(clock, updateIntervalSec);
	}

	@Override
	public final void start() {
		super.start();
		throughputSuccess.resetStartTime();
		throughputFail.resetStartTime();
		reqBytes.resetStartTime();
	}

	@Override
	public final void markElapsedTime(final long millis) {
		prevElapsedTime = millis;
	}

	@Override
	public final void markSucc(final long bytes, final long duration, final long latency) {
		throughputSuccess.mark();
		reqBytes.mark(bytes);
		if(latency > 0 && duration > latency) {
			timingLock.lock();
			try {
				reqDuration.observe(duration);
				respLatency.observe(latency);
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
				reqDuration.observe(duration);
				respLatency.observe(latency);
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
					reqDuration.observe(duration);
					respLatency.observe(latency);
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
					reqDuration.observe(duration);
					respLatency.observe(latency);
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
	@SuppressWarnings("unchecked")
	public void refreshLastSnapshot() {
		final long currentTimeMillis = System.currentTimeMillis();
		final long tsStart = startTimeStamp();
		final long currElapsedTime = tsStart > 0 ? currentTimeMillis - tsStart : 0;
		if(currentTimeMillis - lastOutputTs() > DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS) {
			if(lastDurationSum != reqDurationSum.sum() || lastLatencySum != respLatencySum.sum()) {
				if(timingLock.tryLock()) {
					try {
						reqDurSnapshot = new Snapshot(reqDuration);
						respLatSnapshot = new Snapshot(respLatency);
					} finally {
						timingLock.unlock();
					}
				}
				lastLatencySum = respLatencySum.sum();
				lastDurationSum = reqDurationSum.sum();
			}
			actualConcurrency.observe(actualConcurrencyGauge.getAsInt());
			actualConcurrencySnapshot = new Snapshot(actualConcurrency);
		}
		lastSnapshot = (S) new MetricsSnapshotImpl(throughputSuccess.getCount(), throughputSuccess.
																									  getLastRate(),
			throughputFail.getCount(), throughputFail.getLastRate(), reqBytes.getCount(),
			reqBytes.getLastRate(), tsStart, prevElapsedTime + currElapsedTime, actualConcurrencyGauge.getAsInt(),
			actualConcurrencySnapshot.mean(), concurrencyLimit, lastDurationSum, lastLatencySum, reqDurSnapshot,
			respLatSnapshot
		);
		super.refreshLastSnapshot();
	}

	@Override
	public final long transferSizeSum() {
		return reqBytes.getCount();
	}

	@Override
	protected MetricsContextImpl<S> newThresholdMetricsContext() {
		return new MetricsContextImpl<>(
			id, opType, actualConcurrencyGauge, concurrencyLimit, 0, itemDataSize,
			(int) TimeUnit.MILLISECONDS.toSeconds(outputPeriodMillis), stdOutColorFlag
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(final Object other) {
		if(null == other) {
			return false;
		}
		if(other instanceof MetricsContextImpl) {
			return 0 == compareTo((MetricsContextImpl<S>) other);
		} else {
			return false;
		}
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "(" + opType.name() + '-' + concurrencyLimit + "x1@" + id + ")";
	}

	@Override
	public final void close() {
		prevElapsedTime = System.currentTimeMillis() - startTimeStamp();
		super.close();
	}
}
