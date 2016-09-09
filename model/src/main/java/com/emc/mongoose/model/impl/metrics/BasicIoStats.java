package com.emc.mongoose.model.impl.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.UniformSnapshot;
import com.emc.mongoose.model.api.metrics.IoStats;
import com.emc.mongoose.model.util.SizeInBytes;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 15.09.15.
 */
public class BasicIoStats
implements IoStats {

	protected final String name;
	protected final Clock clock = new ResumableUserTimeClock();
	protected final Histogram reqDuration, respLatency;
	protected final CustomMeter throughputSuccess, throughputFail, reqBytes;
	protected volatile long tsStartMicroSec = -1, prevElapsedTimeMicroSec = 0;
	protected LongAdder reqDurationSum, respLatencySum;
	//
	public BasicIoStats(final String name, final int updateIntervalSec) {
		this.name = name;
		respLatency = new Histogram(new UnsafeButFasterUniformReservoir());
		respLatencySum = new LongAdder();
		reqDuration = new Histogram(new UnsafeButFasterUniformReservoir());
		reqDurationSum = new LongAdder();
		throughputSuccess = new CustomMeter(clock, updateIntervalSec);
		throughputFail = new CustomMeter(clock, updateIntervalSec);
		reqBytes = new CustomMeter(clock, updateIntervalSec);
	}
	//
	@Override
	public void start() {
		tsStartMicroSec = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
		throughputSuccess.resetStartTime();
		throughputFail.resetStartTime();
		reqBytes.resetStartTime();
	}
	//
	@Override
	public final boolean isStarted() {
		return tsStartMicroSec > -1;
	}
	//
	@Override
	public final void markElapsedTime(final long usec) {
		prevElapsedTimeMicroSec = usec;
	}
	//
	@Override
	public void close()
	throws IOException {
	}
	//
	protected final static class BasicSnapshot
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
		private final long elapsedTime;
		//
		public BasicSnapshot(
			final long countSucc, final double succRateLast,
			final long countFail, final double failRate,
			final long countByte, final double byteRate,
			final long elapsedTime, final long sumDur, final long sumLat,
			final com.codahale.metrics.Snapshot durSnapshot,
			final com.codahale.metrics.Snapshot latSnapshot
		) {
			this.countSucc = countSucc;
			this.succRateLast = succRateLast;
			this.countFail = countFail;
			this.failRateLast = failRate;
			this.countByte = countByte;
			this.byteRateLast = byteRate;
			this.sumDur = sumDur;
			this.sumLat = sumLat;
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
			return elapsedTime == 0 ? 0 : M * countSucc / elapsedTime;
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
			return elapsedTime == 0 ? 0 : M * countFail / elapsedTime;
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
			return elapsedTime == 0 ? 0 : M * countByte / elapsedTime;
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
		public final double getDurationAvg() {
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
		public final double getLatencyAvg() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMean();
		}
		//
		@Override
		public final long getElapsedTime() {
			return elapsedTime;
		}
		//
		@Override
		public final String toCountsString() {
			return countSucc + "/" + countFail;
		}
		//
		@Override
		public final String toDurString() {
			return (int) durSnapshot.getMean() + "/" +
				(int) durSnapshot.getMin() + "/" +
				(int) durSnapshot.getMax();
		}
		//
		@Override
		public final String toDurSummaryString() {
			return (int) durSnapshot.getMean() + "/" +
				(int) durSnapshot.getMin() + "/" +
				(int) durSnapshot.getValue(0.25) + "/" +
				(int) durSnapshot.getValue(0.5) + "/" +
				(int) durSnapshot.getValue(0.75) + "/" +
				(int) durSnapshot.getMax();
		}
		//
		@Override
		public final String toLatString() {
			return (int) latSnapshot.getMean() + "/" +
				(int) latSnapshot.getMin() + "/" +
				(int) latSnapshot.getMax();
		}
		//
		@Override
		public final String toLatSummaryString() {
			return (int) latSnapshot.getMean() + "/" +
				(int) latSnapshot.getMin() + "/" +
				(int) latSnapshot.getValue(0.25) + "/" +
				(int) latSnapshot.getValue(0.5) + "/" +
				(int) latSnapshot.getValue(0.75) + "/" +
				(int) latSnapshot.getMax();
		}
		//
		@Override
		public final String toString() {

			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}

			final double elapsedTime = getElapsedTime() / M;
			final String elapsedTimeStr;
			if(0 < elapsedTime && elapsedTime < 1) {
				elapsedTimeStr = String.format(Locale.ROOT, "%.4f", elapsedTime);
			} else if(elapsedTime < 10 ) {
				elapsedTimeStr = String.format(Locale.ROOT, "%.3f", elapsedTime);
			} else if(elapsedTime < 100) {
				elapsedTimeStr = String.format(Locale.ROOT, "%.2f", elapsedTime);
			} else if(elapsedTime < 1000){
				elapsedTimeStr = String.format(Locale.ROOT, "%.1f", elapsedTime);
			} else {
				elapsedTimeStr = Long.toString((long) elapsedTime);
			}

			final double durationSum = getDurationSum() / M;
			final String durationSumStr;
			if(0 < durationSum && durationSum < 1) {
				durationSumStr = String.format(Locale.ROOT, "%.4f", durationSum);
			} else if(durationSum < 10 ) {
				durationSumStr = String.format(Locale.ROOT, "%.3f", durationSum);
			} else if(durationSum < 100) {
				durationSumStr = String.format(Locale.ROOT, "%.2f", durationSum);
			} else if(durationSum < 1000){
				durationSumStr = String.format(Locale.ROOT, "%.1f", durationSum);
			} else {
				durationSumStr = Long.toString((long) durationSum);
			}

			return String.format(
				Locale.ROOT, MSG_FMT_METRICS,
				getSuccCount(), getFailCount(), elapsedTimeStr, durationSumStr,
				getSuccRateMean(), getSuccRateLast(), SizeInBytes.formatFixedSize(getByteCount()),
				getByteRateMean() / MIB, getByteRateLast() / MIB,
				toDurString(), toLatString()
			);
		}
		//
		@Override
		public final String toSummaryString() {

			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}

			final double elapsedTime = getElapsedTime() / M;
			final String elapsedTimeStr;
			if(elapsedTime < 1) {
				elapsedTimeStr = String.format(Locale.ROOT, "%.4f", elapsedTime);
			} else if(elapsedTime < 10 ) {
				elapsedTimeStr = String.format(Locale.ROOT, "%.3f", elapsedTime);
			} else if(elapsedTime < 100) {
				elapsedTimeStr = String.format(Locale.ROOT, "%.2f", elapsedTime);
			} else if(elapsedTime < 1000){
				elapsedTimeStr = String.format(Locale.ROOT, "%.1f", elapsedTime);
			} else {
				elapsedTimeStr = Long.toString((long) elapsedTime);
			}

			final double durationSum = getDurationSum() / M;
			final String durationSumStr;
			if(durationSum < 1) {
				durationSumStr = String.format(Locale.ROOT, "%.4f", durationSum);
			} else if(durationSum < 10 ) {
				durationSumStr = String.format(Locale.ROOT, "%.3f", durationSum);
			} else if(durationSum < 100) {
				durationSumStr = String.format(Locale.ROOT, "%.2f", durationSum);
			} else if(durationSum < 1000){
				durationSumStr = String.format(Locale.ROOT, "%.1f", durationSum);
			} else {
				durationSumStr = Long.toString((long) durationSum);
			}

			return String.format(
				Locale.ROOT, MSG_FMT_METRICS,
				getSuccCount(), getFailCount(), elapsedTimeStr, durationSumStr,
				getSuccRateMean(), getSuccRateLast(), SizeInBytes.formatFixedSize(getByteCount()),
				getByteRateMean() / MIB, getByteRateLast() / MIB,
				toDurSummaryString(), toLatSummaryString()
			);
		}
	}
	//
	@Override
	public void markSucc(final long size, final int duration, final int latency) {
		throughputSuccess.mark();
		reqBytes.mark(size);
		reqDuration.update(duration);
		reqDurationSum.add(duration);
		respLatencySum.add(latency);
		respLatency.update(latency);
	}
	//
	@Override
	public void markSucc(
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
	public void markFail() {
		throughputFail.mark();
	}
	//
	@Override
	public void markFail(final long count) {
		throughputFail.mark(count);
	}
	//
	@Override
	public Snapshot getSnapshot() {
		final long currElapsedTime = tsStartMicroSec > 0 ?
			TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) - tsStartMicroSec : 0;
		final com.codahale.metrics.Snapshot reqDurSnapshot = reqDuration.getSnapshot();
		final com.codahale.metrics.Snapshot respLatSnapshot = respLatency.getSnapshot();
		return new BasicSnapshot(
			throughputSuccess.getCount(), throughputSuccess.getLastRate(), throughputFail.getCount(),
			throughputFail.getLastRate(), reqBytes.getCount(), reqBytes.getLastRate(),
			prevElapsedTimeMicroSec + currElapsedTime, reqDurationSum.sum(), respLatencySum.sum(),
			reqDurSnapshot, respLatSnapshot
		);
	}
}
