package com.emc.mongoose.metrics.context;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.MetricsSnapshotImpl;
import com.emc.mongoose.metrics.util.Histogram;
import com.emc.mongoose.metrics.util.HistogramSnapshotImpl;
import com.emc.mongoose.metrics.util.MeterImpl;
import com.github.akurilov.commons.system.SizeInBytes;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;

import static com.emc.mongoose.Constants.METRIC_LABEL_CONC;
import static com.emc.mongoose.Constants.METRIC_LABEL_ID;
import static com.emc.mongoose.Constants.METRIC_LABEL_OP_TYPE;
import static com.emc.mongoose.Constants.METRIC_LABEL_SIZE;
import static com.emc.mongoose.Constants.METRIC_NAME_CONC;
import static com.emc.mongoose.Constants.METRIC_NAME_DUR;
import static com.emc.mongoose.Constants.METRIC_NAME_LAT;

public class MetricsContextImpl<S extends MetricsSnapshotImpl>
	extends MetricsContextBase<S>
	implements MetricsContext<S> {

	private final String[] labelNames = { METRIC_LABEL_ID, METRIC_LABEL_OP_TYPE, METRIC_LABEL_SIZE, METRIC_LABEL_CONC };
	private final Clock clock = Clock.systemDefaultZone();
	private final Histogram reqDuration, respLatency, actualConcurrency;
	private final LongAdder reqDurationSum, respLatencySum;
	private final MeterImpl throughputSuccess, throughputFail, reqBytes;
	private final IntSupplier actualConcurrencyGauge;
	private final Lock timingLock = new ReentrantLock();
	private volatile long prevElapsedTime = 0;
	private volatile long lastDurationSum = 0;
	private volatile long lastLatencySum = 0;
	private volatile HistogramSnapshotImpl reqDurSnapshot, respLatSnapshot, actualConcurrencySnapshot;

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
		final String[] labelValues = { id, opType.name(), itemDataSize.toString(), String.valueOf(concurrencyLimit) };
		//
		respLatency = new Histogram(DEFAULT_RESERVOIR_SIZE)
			.name(METRIC_NAME_DUR)
			.labels(labelNames, labelValues)
			.register();
		respLatSnapshot = respLatency.snapshot();
		respLatencySum = new LongAdder();
		//
		reqDuration = new Histogram(DEFAULT_RESERVOIR_SIZE)
			.name(METRIC_NAME_LAT)
			.labels(labelNames, labelValues)
			.register();
		reqDurSnapshot = reqDuration.snapshot();
		reqDurationSum = new LongAdder();
		//
		actualConcurrency = new Histogram(DEFAULT_RESERVOIR_SIZE)
			.name(METRIC_NAME_CONC)
			.labels(labelNames, labelValues)
			.register();
		actualConcurrencySnapshot = actualConcurrency.snapshot();
		//
		throughputSuccess = new MeterImpl(clock, updateIntervalSec);
		throughputFail = new MeterImpl(clock, updateIntervalSec);
		reqBytes = new MeterImpl(clock, updateIntervalSec);
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
		for(int i = 0; i < timingsLen; ++ i) {
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
		for(int i = 0; i < timingsLen; ++ i) {
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
						reqDurSnapshot = reqDuration.snapshot();
						respLatSnapshot = respLatency.snapshot();
					} finally {
						timingLock.unlock();
					}
				}
				lastLatencySum = respLatencySum.sum();
				lastDurationSum = reqDurationSum.sum();
			}
			actualConcurrency.update(actualConcurrencyGauge.getAsInt());
			actualConcurrencySnapshot = actualConcurrency.snapshot();
		}
		lastSnapshot = (S) new MetricsSnapshotImpl(throughputSuccess.count(), throughputSuccess.lastRate(),
			throughputFail.count(), throughputFail.lastRate(), reqBytes.count(),
			reqBytes.lastRate(), tsStart, prevElapsedTime + currElapsedTime, actualConcurrencyGauge.getAsInt(),
			actualConcurrencySnapshot.mean(), concurrencyLimit, reqDurSnapshot,
			respLatSnapshot
		);
		super.refreshLastSnapshot();
	}

	@Override
	public final long transferSizeSum() {
		return reqBytes.count();
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
