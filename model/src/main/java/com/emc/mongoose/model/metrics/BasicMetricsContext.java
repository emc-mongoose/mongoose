package com.emc.mongoose.model.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.UniformSnapshot;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.io.IoType;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 15.09.15.
 Start timestamp and elapsed time is in milliseconds while other time values are in microseconds.
 */
public final class BasicMetricsContext
implements Comparable<BasicMetricsContext>, MetricsContext {

	private final Clock clock = new ResumableUserTimeClock();
	private final Histogram reqDuration, respLatency;
	private volatile com.codahale.metrics.Snapshot reqDurSnapshot, respLatSnapshot;
	private final LongAdder reqDurationSum, respLatencySum;
	private volatile long lastDurationSum = 0, lastLatencySum = 0;
	private final CustomMeter throughputSuccess, throughputFail, reqBytes;
	private final long ts;
	private volatile long tsStart = -1, prevElapsedTime = 0;
	
	private final String stepName;
	private final IoType ioType;
	private final int driverCount;
	private final int concurrency;
	private final int thresholdConcurrency;
	private final SizeInBytes itemSize;
	private final boolean volatileOutputFlag;
	private final long outputPeriodMillis;
	private volatile long lastOutputTs = System.currentTimeMillis();
	private volatile Snapshot lastSnapshot = null;
	private volatile MetricsContext thresholdMetricsCtx = null;
	private volatile boolean thresholdStateExitedFlag = false;

	//
	public BasicMetricsContext(
		final String stepName, final IoType ioType, final int driverCount, final int concurrency,
		final int thresholdConcurrency, final SizeInBytes itemSize,
		final boolean volatileOutputFlag, final int updateIntervalSec
	) {
		this.stepName = stepName;
		this.ioType = ioType;
		this.driverCount = driverCount;
		this.concurrency = concurrency;
		this.thresholdConcurrency = thresholdConcurrency > 0 ?
			thresholdConcurrency : Integer.MAX_VALUE;
		this.itemSize = itemSize;
		this.volatileOutputFlag = volatileOutputFlag;
		this.outputPeriodMillis = TimeUnit.SECONDS.toMillis(updateIntervalSec);
		respLatency = new Histogram(new SlidingWindowReservoir(0x1_00_00));
		respLatSnapshot = respLatency.getSnapshot();
		respLatencySum = new LongAdder();
		reqDuration = new Histogram(new SlidingWindowReservoir(0x1_00_00));
		reqDurSnapshot = reqDuration.getSnapshot();
		reqDurationSum = new LongAdder();
		throughputSuccess = new CustomMeter(clock, updateIntervalSec);
		throughputFail = new CustomMeter(clock, updateIntervalSec);
		reqBytes = new CustomMeter(clock, updateIntervalSec);
		ts = System.nanoTime();
	}
	//
	@Override
	public final void start() {
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
	public final String getStepName() {
		return stepName;
	}
	//
	@Override
	public final IoType getIoType() {
		return ioType;
	}
	//
	@Override
	public final int getDriverCount() {
		return driverCount;
	}
	//
	@Override
	public final int getConcurrency() {
		return concurrency;
	}
	//
	@Override
	public final int getConcurrencyThreshold() {
		return thresholdConcurrency;
	}
	//
	@Override
	public final SizeInBytes getItemSize() {
		return itemSize;
	}
	//
	@Override
	public final boolean getVolatileOutputFlag() {
		return volatileOutputFlag;
	}
	//
	@Override
	public final long getOutputPeriodMillis() {
		return outputPeriodMillis;
	}
	//
	@Override
	public final long getLastOutputTs() {
		return lastOutputTs;
	}
	
	@Override
	public final void setLastOutputTs(final long ts) {
		lastOutputTs = ts;
	}
	//
	@Override
	public final void refreshLastSnapshot() {
		final long currElapsedTime = tsStart > 0 ? System.currentTimeMillis() - tsStart : 0;
		if(lastDurationSum != reqDurationSum.sum()) {
			lastDurationSum = reqDurationSum.sum();
			reqDurSnapshot = reqDuration.getSnapshot();
		}
		if(lastLatencySum != respLatencySum.sum()) {
			lastLatencySum = respLatencySum.sum();
			respLatSnapshot = respLatency.getSnapshot();
		}
		lastSnapshot =  new BasicSnapshot(
			throughputSuccess.getCount(), throughputSuccess.getLastRate(),
			throughputFail.getCount(), throughputFail.getLastRate(), reqBytes.getCount(),
			reqBytes.getLastRate(), tsStart, prevElapsedTime + currElapsedTime,
			reqDurationSum.sum(), respLatencySum.sum(), reqDurSnapshot, respLatSnapshot
		);
		if(thresholdMetricsCtx != null) {
			thresholdMetricsCtx.refreshLastSnapshot();
		}
	}
	//
	@Override
	public final Snapshot getLastSnapshot() {
		if(lastSnapshot == null) {
			refreshLastSnapshot();
		}
		return lastSnapshot;
	}
	//
	@Override
	public final boolean isThresholdStateEntered() {
		return thresholdMetricsCtx != null && thresholdMetricsCtx.isStarted();
	}
	//
	@Override
	public final void enterThresholdState()
	throws IllegalStateException {
		if(thresholdMetricsCtx != null) {
			throw new IllegalStateException("Nested metrics context already exists");
		}
		thresholdMetricsCtx = new BasicMetricsContext(
			stepName, ioType, driverCount, concurrency, 0, itemSize, volatileOutputFlag,
			(int) TimeUnit.MILLISECONDS.toSeconds(outputPeriodMillis)
		);
		thresholdMetricsCtx.start();
	}
	//
	@Override
	public final MetricsContext getThresholdMetrics()
	throws IllegalStateException {
		if(thresholdMetricsCtx == null) {
			throw new IllegalStateException("Nested metrics context is not exist");
		}
		return thresholdMetricsCtx;
	}
	//
	@Override
	public final boolean isThresholdStateExited() {
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
	public final int compareTo(final BasicMetricsContext other) {
		return Long.compare(ts, other.ts);
	}
	//
	@Override
	public final String toString() {
		return "MetricsContext(" + ioType.name() + '-' + concurrency + 'x' + driverCount  + '@' +
			stepName + ")";
	}
	//
	protected static final class BasicSnapshot
	implements Snapshot {
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
		private final long startTime;
		private final long elapsedTime;
		//
		public BasicSnapshot(
			final long countSucc, final double succRateLast, final long countFail,
			final double failRateLast, final long countByte, final double byteRateLast,
			final long startTime, final long elapsedTime, final long sumDur,
			final long sumLat, final com.codahale.metrics.Snapshot durSnapshot,
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
			this.startTime = startTime;
			this.elapsedTime = elapsedTime;
			this.durSnapshot = durSnapshot;
			this.durValues = durSnapshot.getValues();
			this.latSnapshot = latSnapshot;
			this.latValues = latSnapshot.getValues();
		}
		//
		@Override
		public final long getSuccCount() {
			return countSucc;
		}
		//
		@Override
		public final double getSuccRateMean() {
			return elapsedTime == 0 ? 0 : 1000.0 * countSucc / elapsedTime;
		}
		//
		@Override
		public final double getSuccRateLast() {
			return succRateLast;
		}
		//
		@Override
		public final long getFailCount() {
			return countFail;
		}
		//
		@Override
		public final double getFailRateMean() {
			return elapsedTime == 0 ? 0 : 1000.0 * countFail / elapsedTime;
		}
		//
		@Override
		public final double getFailRateLast() {
			return failRateLast;
		}
		//
		@Override
		public final long getByteCount() {
			return countByte;
		}
		//
		@Override
		public final double getByteRateMean() {
			return elapsedTime == 0 ? 0 : 1000.0 * countByte / elapsedTime;
		}
		//
		@Override
		public final double getByteRateLast() {
			return byteRateLast;
		}
		//
		@Override
		public final long getDurationMin() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getMin();
		}
		//
		@Override
		public final long getDurationLoQ() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return (long) durSnapshot.getValue(0.25);
		}
		//
		@Override
		public final long getDurationMed() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return (long) durSnapshot.getValue(0.5);
		}
		//
		@Override
		public final long getDurationHiQ() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return (long) durSnapshot.getValue(0.75);
		}
		//
		@Override
		public final long getDurationMax() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getMax();
		}
		//
		@Override
		public final long getDurationSum() {
			return sumDur;
		}
		//
		@Override
		public final long[] getDurationValues() {
			return durValues;
		}
		//
		@Override
		public final double getDurationMean() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getMean();
		}
		//
		@Override
		public final long getLatencyMin() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMin();
		}
		//
		@Override
		public final long getLatencyLoQ() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return (long) latSnapshot.getValue(0.25);
		}
		//
		@Override
		public final long getLatencyMed() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return (long) latSnapshot.getValue(0.5);
		}
		//
		@Override
		public final long getLatencyHiQ() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return (long) latSnapshot.getValue(0.75);
		}//
		@Override
		public final long getLatencyMax() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMax();
		}
		//
		@Override
		public final long[] getLatencyValues() {
			return latValues;
		}
		//
		@Override
		public final long getLatencySum() {
			return sumDur;
		}
		//
		@Override
		public final double getLatencyMean() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMean();
		}
		//
		@Override
		public final long getStartTime() {
			return startTime;
		}
		
		@Override
		public final long getElapsedTime() {
			return elapsedTime;
		}
	}
}
