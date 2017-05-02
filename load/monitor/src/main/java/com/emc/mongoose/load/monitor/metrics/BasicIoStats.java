package com.emc.mongoose.load.monitor.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.UniformSnapshot;
import com.emc.mongoose.model.metrics.CustomMeter;
import com.emc.mongoose.model.metrics.ResumableUserTimeClock;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 15.09.15.
 Start timestamp and elapsed time is in milliseconds while other time values are in microseconds.
 */
public final class BasicIoStats
implements IoStats {

	protected final String name;
	protected final Clock clock = new ResumableUserTimeClock();
	protected final Histogram reqDuration, respLatency;
	protected final CustomMeter throughputSuccess, throughputFail, reqBytes;
	protected volatile long tsStart = -1, prevElapsedTime = 0;
	protected final LongAdder reqDurationSum, respLatencySum;
	//
	public BasicIoStats(final String name, final int updateIntervalSec) {
		this.name = name;
		respLatency = new Histogram(new SlidingWindowReservoir(0x10_00_00));
		respLatencySum = new LongAdder();
		reqDuration = new Histogram(new SlidingWindowReservoir(0x10_00_00));
		reqDurationSum = new LongAdder();
		throughputSuccess = new CustomMeter(clock, updateIntervalSec);
		throughputFail = new CustomMeter(clock, updateIntervalSec);
		reqBytes = new CustomMeter(clock, updateIntervalSec);
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
	}
	//
	@Override
	public final void markFail() {
		throughputFail.mark();
	}
	//
	@Override
	public final void markFail(final long count) {
		throughputFail.mark(count);
	}
	//
	@Override
	public final Snapshot getSnapshot() {
		final long currElapsedTime = tsStart > 0 ? System.currentTimeMillis() - tsStart : 0;
		final com.codahale.metrics.Snapshot reqDurSnapshot = reqDuration.getSnapshot();
		final com.codahale.metrics.Snapshot respLatSnapshot = respLatency.getSnapshot();
		return new BasicSnapshot(
			throughputSuccess.getCount(), throughputSuccess.getLastRate(),
			throughputFail.getCount(), throughputFail.getLastRate(), reqBytes.getCount(),
			reqBytes.getLastRate(), tsStart, prevElapsedTime + currElapsedTime,
			reqDurationSum.sum(), respLatencySum.sum(), reqDurSnapshot, respLatSnapshot
		);
	}
}
