package com.emc.mongoose.core.impl.load.model.metrics;
//
import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.UniformReservoir;
import com.codahale.metrics.UniformSnapshot;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private final static Logger LOG = LogManager.getLogger();
	private final static double M = 1e6;
	//
	protected final String name;
	protected final Clock clock = new ResumableUserTimeClock();
	protected final CustomMetricRegistry metrics = new CustomMetricRegistry();
	protected final MBeanServer mBeanServer;
	protected final CustomJmxReporter jmxReporter;
	protected final Histogram reqDuration, respLatency;
	protected volatile long tsStartMicroSec = -1, prevElapsedTimeMicroSec = 0;
	//
	protected IOStatsBase(final String name, final int serveJmxPort) {
		this.name = name;
		if(serveJmxPort > 0) {
			mBeanServer = ServiceUtil.getMBeanServer(serveJmxPort);
			jmxReporter = CustomJmxReporter.forRegistry(metrics).registerWith(mBeanServer).build();
		} else {
			mBeanServer = null;
			jmxReporter = null;
		}
		respLatency = metrics.register(
			CustomMetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_LAT),
			new Histogram(new UniformReservoir())
		);
		reqDuration = metrics.register(
			CustomMetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_DUR),
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
	protected static class BasicSnapshot
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
		private final long elapsedTime;
		//
		public BasicSnapshot(
			final long countSucc, final double succRateLast,
			final long countFail, final double failRate,
			final long countByte, final double byteRate,
			final long elapsedTime, final long sumDur,
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
			this.elapsedTime = elapsedTime;
			this.durSnapshot = durSnapshot;
			this.durValues = durSnapshot.getValues();
			this.latSnapshot = latSnapshot;
			this.latValues = latSnapshot.getValues();
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
		public double getSuccRateLast() {
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
		public double getByteRateLast() {
			return byteRateLast;
		}
		//
		@Override
		public double getDurationMin() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getMin();
		}
		//
		@Override
		public double getDurationLoQ() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getValue(0.25);
		}
		//
		@Override
		public double getDurationMed() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getValue(0.5);
		}
		//
		@Override
		public double getDurationHiQ() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getValue(0.75);
		}
		//
		@Override
		public double getDurationMax() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			return durSnapshot.getMax();
		}
		//
		@Override
		public long getDurationSum() {
			return sumDur;
		}
		//
		@Override
		public long[] getDurationValues() {
			return durValues;
		}
		//
		@Override
		public double getLatencyMin() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMin();
		}
		//
		@Override
		public double getLatencyLoQ() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getValue(0.25);
		}
		//
		@Override
		public double getLatencyMed() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getValue(0.5);
		}
		//
		@Override
		public double getLatencyHiQ() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getValue(0.75);
		}//
		@Override
		public double getLatencyMax() {
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return latSnapshot.getMax();
		}
		//
		@Override
		public long[] getLatencyValues() {
			return latValues;
		}
		//
		@Override
		public long getElapsedTime() {
			return elapsedTime;
		}
		//
		@Override
		public String toString() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
				countSucc,
				LogUtil.isConsoleColoringEnabled() ?
					countFail == 0 ?
						Long.toString(countFail) :
						(float) countSucc / countFail > 1000 ?
							String.format(LogUtil.INT_YELLOW_OVER_GREEN, countFail) :
							String.format(LogUtil.INT_RED_OVER_GREEN, countFail) :
					Long.toString(countFail),
				//
				(int) durSnapshot.getMean(),
				(int) durSnapshot.getMin(),
				(int) durSnapshot.getMax(),
				//
				(int) latSnapshot.getMean(),
				(int) latSnapshot.getMin(),
				(int) latSnapshot.getMax(),
				//
				getSuccRateMean(), succRateLast, getByteRateMean() / MIB, byteRateLast / MIB
			);
		}
		//
		@Override
		public String toSummaryString() {
			if(durSnapshot == null) {
				durSnapshot = new UniformSnapshot(durValues);
			}
			if(latSnapshot == null) {
				latSnapshot = new UniformSnapshot(latValues);
			}
			return String.format(
				LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS_SUM,
				countSucc,
				LogUtil.isConsoleColoringEnabled() ?
					countFail == 0 ?
						Long.toString(countFail) :
						(float) countSucc / countFail > 1000 ?
							String.format(LogUtil.INT_YELLOW_OVER_GREEN, countFail) :
							String.format(LogUtil.INT_RED_OVER_GREEN, countFail) :
					Long.toString(countFail),
				//
				(int) durSnapshot.getMean(),
				(int) durSnapshot.getMin(),
				(int) durSnapshot.getValue(0.25),
				(int) durSnapshot.getValue(0.5),
				(int) durSnapshot.getValue(0.75),
				(int) durSnapshot.getMax(),
				//
				(int) latSnapshot.getMean(),
				(int) latSnapshot.getMin(),
				(int) latSnapshot.getValue(0.25),
				(int) latSnapshot.getValue(0.5),
				(int) latSnapshot.getValue(0.75),
				(int) latSnapshot.getMax(),
				//
				getSuccRateMean(), succRateLast,
				getByteRateMean() / MIB, byteRateLast / MIB
			);
		}
	}
}
