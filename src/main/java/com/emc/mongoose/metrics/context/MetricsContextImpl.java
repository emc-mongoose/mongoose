package com.emc.mongoose.metrics.context;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.MetricsSnapshotImpl;
import com.emc.mongoose.metrics.util.RateMeter;
import com.emc.mongoose.metrics.util.RateMeterImpl;
import com.emc.mongoose.metrics.util.RateMetricSnapshot;
import com.emc.mongoose.metrics.util.TimingMeter;
import com.emc.mongoose.metrics.util.TimingMeterImpl;
import com.emc.mongoose.metrics.util.TimingMetricSnapshot;
import com.github.akurilov.commons.system.SizeInBytes;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;

import static com.emc.mongoose.Constants.METRIC_NAME_BYTE;
import static com.emc.mongoose.Constants.METRIC_NAME_CONC;
import static com.emc.mongoose.Constants.METRIC_NAME_DUR;
import static com.emc.mongoose.Constants.METRIC_NAME_FAIL;
import static com.emc.mongoose.Constants.METRIC_NAME_LAT;
import static com.emc.mongoose.Constants.METRIC_NAME_SUCC;

public class MetricsContextImpl<S extends MetricsSnapshotImpl>
	extends MetricsContextBase<S>
	implements MetricsContext<S> {

	private final Clock clock = Clock.systemDefaultZone();
	private final TimingMeter<TimingMetricSnapshot> reqDuration, respLatency, actualConcurrency;
	private final RateMeter<RateMetricSnapshot> throughputSuccess, throughputFail, reqBytes;
	private volatile TimingMetricSnapshot reqDurSnapshot, respLatSnapshot, actualConcurrencySnapshot;
	private final IntSupplier actualConcurrencyGauge;
	private final Lock timingLock = new ReentrantLock();
	private volatile long lastDurationSum = 0;
	private volatile long lastLatencySum = 0;

	public MetricsContextImpl(
		final String id, final OpType opType, final IntSupplier actualConcurrencyGauge, final int concurrencyLimit,
		final int concurrencyThreshold, final SizeInBytes itemDataSize, final int updateIntervalSec,
		final boolean stdOutColorFlag
	) {
		super(
			id, opType, concurrencyLimit, 1, concurrencyThreshold, itemDataSize, stdOutColorFlag,
			TimeUnit.SECONDS.toMillis(updateIntervalSec)
		);
		//
		respLatency = new TimingMeterImpl<>(DEFAULT_RESERVOIR_SIZE, METRIC_NAME_LAT);
		respLatSnapshot = respLatency.snapshot();
		//
		reqDuration = new TimingMeterImpl<>(DEFAULT_RESERVOIR_SIZE, METRIC_NAME_DUR);
		reqDurSnapshot = reqDuration.snapshot();
		//
		this.actualConcurrencyGauge = actualConcurrencyGauge;
		actualConcurrency = new TimingMeterImpl<>(DEFAULT_RESERVOIR_SIZE, METRIC_NAME_CONC);
		actualConcurrencySnapshot = actualConcurrency.snapshot();
		//
		throughputSuccess = new RateMeterImpl<>(clock, updateIntervalSec, METRIC_NAME_SUCC);
		//
		throughputFail = new RateMeterImpl<>(clock, updateIntervalSec, METRIC_NAME_FAIL);
		//
		reqBytes = new RateMeterImpl<>(clock, updateIntervalSec, METRIC_NAME_BYTE);
	}

	@Override
	public final void start() {
		super.start();
		throughputSuccess.resetStartTime();
		throughputFail.resetStartTime();
		reqBytes.resetStartTime();
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
		if(currentTimeMillis - lastOutputTs() > DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS) {
			if(lastDurationSum != reqDuration.sum() || lastLatencySum != respLatency.sum()) {
				if(timingLock.tryLock()) {
					try {
						reqDurSnapshot = reqDuration.snapshot();
						respLatSnapshot = respLatency.snapshot();
					} finally {
						timingLock.unlock();
					}
				}
				lastLatencySum = respLatency.sum();
				lastDurationSum = reqDuration.sum();
			}
			actualConcurrency.update(actualConcurrencyGauge.getAsInt());
			actualConcurrencySnapshot = actualConcurrency.snapshot();
		}
		lastSnapshot = (S) new MetricsSnapshotImpl(
			reqDurSnapshot, respLatSnapshot, actualConcurrencySnapshot, throughputFail.snapshot(),
			throughputSuccess.snapshot(), reqBytes.snapshot(), elapsedTimeMillis()
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
		super.close();
	}
}
