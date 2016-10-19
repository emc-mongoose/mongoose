package com.emc.mongoose.core.impl.load.model.metrics;
//
import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.UniformReservoir;
import com.codahale.metrics.UniformSnapshot;
//
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.load.model.metrics.IoStats;
//
import javax.management.MBeanServer;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 15.09.15.
 */
public abstract class IoStatsBase
implements IoStats {
	//
	private final static double M = 1e6;
	private final static int DEFAULT_JMX_PORT = 1199;
	//
	protected final String name;
	protected final Clock clock = new ResumableUserTimeClock();
	protected final CustomMetricRegistry metrics = new CustomMetricRegistry();
	protected final MBeanServer mBeanServer;
	protected final CustomJmxReporter jmxReporter;
	protected final Histogram reqDuration, respLatency;
	protected volatile long tsStartMilliSec = -1;
	protected volatile long tsStartMicroSec = -1, prevElapsedTimeMicroSec = 0;
	//
	protected IoStatsBase(final String name, final boolean serveJmxFlag) {
		this.name = name;
		if(serveJmxFlag) {
			mBeanServer = ServiceUtil.getMBeanServer(DEFAULT_JMX_PORT);
			jmxReporter = CustomJmxReporter.forRegistry(metrics).registerWith(mBeanServer).build();
		} else {
			mBeanServer = null;
			jmxReporter = null;
		}
		respLatency = new Histogram(new UniformReservoir());
		reqDuration = new Histogram(new UniformReservoir());
	}
	//
	@Override
	public void start() {
		if(jmxReporter != null) {
			jmxReporter.start();
		}
		tsStartMicroSec = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
		tsStartMilliSec = System.currentTimeMillis();
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
		if(jmxReporter != null) {
			jmxReporter.stop();
		}
		metrics.close();
	}
	//
	private final static NumberFormat NF_0_10 = NumberFormat.getInstance(Locale.ROOT);
	private final static NumberFormat NF_10_100 = NumberFormat.getInstance(Locale.ROOT);
	private final static NumberFormat NF_100_AND_MORE = NumberFormat.getInstance(Locale.ROOT);
	static {
		NF_0_10.setGroupingUsed(false);
		NF_0_10.setMinimumFractionDigits(3);
		NF_0_10.setMaximumFractionDigits(3);
		NF_10_100.setGroupingUsed(false);
		NF_10_100.setMinimumFractionDigits(2);
		NF_10_100.setMaximumFractionDigits(2);
		NF_100_AND_MORE.setGroupingUsed(false);
		NF_100_AND_MORE.setMinimumFractionDigits(1);
		NF_100_AND_MORE.setMaximumFractionDigits(1);
	}
	public static String formatNumber(final double number) {
		if(number == Math.floor(number) && !Double.isInfinite(number)) {
			return Long.toString((long) number);
		} else if(number < 0) {
			return "-" + formatNumber(-number);
		} else if(number < 10) {
			return NF_0_10.format(number);
		} else if(number < 100) {
			return NF_10_100.format(number);
		} else if(number < 1000) {
			return NF_100_AND_MORE.format(number);
		} else {
			return Long.toString((long) number);
		}
	}
	//
	protected final static class BasicSnapshot
	implements Snapshot {
		//
		private final long tsStartMilliSec;
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
			final long tsStartMilliSec,
			final long countSucc, final double succRateLast,
			final long countFail, final double failRate,
			final long countByte, final double byteRate,
			final long elapsedTime, final long sumDur, final long sumLat,
			final com.codahale.metrics.Snapshot durSnapshot,
			final com.codahale.metrics.Snapshot latSnapshot
		) {
			this.tsStartMilliSec = tsStartMilliSec;
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
		public final long getStartTimeMilliSec() {
			return tsStartMilliSec;
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
			return countSucc + "/" +
				(
					LogUtil.isConsoleColoringEnabled() ?
						countFail == 0 ?
							Long.toString(countFail) :
							(float) countSucc / countFail > 1000 ?
								String.format(LogUtil.INT_YELLOW_OVER_GREEN, countFail) :
								String.format(LogUtil.INT_RED_OVER_GREEN, countFail) :
						Long.toString(countFail)
				);
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
		public final String toSuccRatesString() {
			return String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_PAIR, getSuccRateMean(), succRateLast
			);
		}
		//
		@Override
		public final String toByteRatesString() {
			return String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_PAIR, getByteRateMean() / MIB, byteRateLast / MIB
			);
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

			return String.format(
				Locale.ROOT, MSG_FMT_METRICS,
				getSuccCount(), getFailCount(), SizeInBytes.formatFixedSize(getByteCount()),
				formatNumber(getElapsedTime() / M), formatNumber(getDurationSum() / M),
				formatNumber(getSuccRateMean()), formatNumber(getSuccRateLast()),
				formatNumber(getByteRateMean() / MIB), formatNumber(getByteRateLast() / MIB),
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

			return String.format(
				Locale.ROOT, MSG_FMT_METRICS,
				getSuccCount(), getFailCount(), SizeInBytes.formatFixedSize(getByteCount()),
				formatNumber(getElapsedTime() / M), formatNumber(getDurationSum() / M),
				formatNumber(getSuccRateMean()), formatNumber(getSuccRateLast()),
				formatNumber(getByteRateMean() / MIB), formatNumber(getByteRateLast() / MIB),
				toDurSummaryString(), toLatSummaryString()
			);
		}
	}
}
