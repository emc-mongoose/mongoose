package com.emc.mongoose.core.impl.load.model.metrics;
//
import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.UniformReservoir;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import javax.management.MBeanServer;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 15.09.15.
 */
public abstract class IOStatsBase
implements IOStats {
	//
	private final static double M = 1e6;
	//
	protected final String name;
	protected final Clock clock = new ResumableUserTimeClock();
	protected final MetricRegistry metrics = new MetricRegistry();
	protected final MBeanServer mBeanServer;
	protected final JmxReporter jmxReporter;
	protected final Histogram reqDuration, respLatency;
	protected volatile long tsStartMicroSec = -1, prevElapsedTimeMicroSec = 0;
	//
	protected IOStatsBase(final String name, final int serveJmxPort) {
		this.name = name;
		if(serveJmxPort > 0) {
			mBeanServer = ServiceUtils.getMBeanServer(serveJmxPort);
			jmxReporter = JmxReporter.forRegistry(metrics).registerWith(mBeanServer).build();
		} else {
			mBeanServer = null;
			jmxReporter = null;
		}
		respLatency = metrics.register(
			MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_LAT),
			new Histogram(new UniformReservoir())
		);
		reqDuration = metrics.register(
			MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_DUR),
			new Histogram(new UniformReservoir())
		);
	}
	//
	@Override
	public void start() {
		if(jmxReporter != null) {
			jmxReporter.start();
		}
		tsStartMicroSec = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
	}
	//
	@Override
	public void markElapsedTime(final long usec) {
		prevElapsedTimeMicroSec = usec;
	}
	//
	@Override
	public void close()
	throws IOException {
		if(jmxReporter != null) {
			jmxReporter.stop();
		}
	}
	//
	public static class BasicSnapshot
	implements Snapshot {
		//
		private final long countSucc;
		private final double succRateLast;
		private final long countFail;
		private final double failRateLast;
		private final long countByte;
		private final double byteRateLast;
		private final com.codahale.metrics.Snapshot snapshotDur;
		private final com.codahale.metrics.Snapshot snapshotLat;
		private final long sumDur;
		private final long elapsedTime;
		//
		public BasicSnapshot(
			final long countSucc, final double succRateLast,
			final long countFail, final double failRate,
			final long countByte, final double byteRate,
			final long sumDur, final long elapsedTime,
			final com.codahale.metrics.Snapshot snapshotDur,
			final com.codahale.metrics.Snapshot snapshotLat
		) {
			this.countSucc = countSucc;
			this.succRateLast = succRateLast;
			this.countFail = countFail;
			this.failRateLast = failRate;
			this.countByte = countByte;
			this.byteRateLast = byteRate;
			this.sumDur = sumDur;
			this.elapsedTime = elapsedTime;
			this.snapshotDur = snapshotDur;
			this.snapshotLat = snapshotLat;
		}
		//
		@Override
		public long getSuccCount() {
			return countSucc;
		}
		//
		@Override
		public double getSuccRateMean() {
			return elapsedTime == 0 ? 0 : M * countSucc / elapsedTime;
		}
		//
		@Override
		public double getSuccRate() {
			return succRateLast;
		}
		//
		@Override
		public long getFailCount() {
			return countFail;
		}
		//
		@Override
		public double getFailRateMean() {
			return elapsedTime == 0 ? 0 : M * countFail / elapsedTime;
		}
		//
		@Override
		public double getFailRateLast() {
			return failRateLast;
		}
		//
		@Override
		public long getByteCount() {
			return countByte;
		}
		//
		@Override
		public double getByteRateMean() {
			return elapsedTime == 0 ? 0 : M * countByte / elapsedTime;
		}
		//
		@Override
		public double getByteRate() {
			return byteRateLast;
		}
		//
		@Override
		public double getDurationMean() {
			return snapshotDur.getMean();
		}
		//
		@Override
		public double getDurationMin() {
			return snapshotDur.getMin();
		}
		//
		@Override
		public double getDurationStdDev() {
			return snapshotDur.getStdDev();
		}
		//
		@Override
		public double getDurationMax() {
			return snapshotDur.getMax();
		}
		//
		@Override
		public long getDurationSum() {
			return sumDur;
		}
		//
		@Override
		public long[] getDurationValues() {
			return snapshotDur.getValues();
		}
		//
		@Override
		public double getLatencyMean() {
			return snapshotLat.getMean();
		}
		//
		@Override
		public double getLatencyMin() {
			return snapshotLat.getMin();
		}
		//
		@Override
		public double getLatencyStdDev() {
			return snapshotLat.getStdDev();
		}
		//
		@Override
		public double getLatencyMax() {
			return snapshotLat.getMax();
		}
		//
		@Override
		public long[] getLatencyValues() {
			return snapshotLat.getValues();
		}
		//
		@Override
		public long getElapsedTime() {
			return elapsedTime;
		}
		//
		@Override
		public String toString() {
			return String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
				countSucc,
				countFail == 0 ?
					Long.toString(countFail) :
					(float) countSucc / countFail > 100 ?
						String.format(LogUtil.INT_YELLOW_OVER_GREEN, countFail) :
						String.format(LogUtil.INT_RED_OVER_GREEN, countFail),
				//
				(int) snapshotDur.getMean(),
				(int) snapshotDur.getMin(),
				(int) snapshotDur.getStdDev(),
				(int) snapshotDur.getMax(),
				//
				(int) snapshotLat.getMean(),
				(int) snapshotLat.getMin(),
				(int) snapshotLat.getStdDev(),
				(int) snapshotLat.getMax(),
				//
				getSuccRateMean(), succRateLast, getByteRateMean() / MIB, byteRateLast / MIB
			);
		}
	}
}
