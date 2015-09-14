package com.emc.mongoose.core.impl.load.model.metrics;
//
import com.codahale.metrics.Clock;
//
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.UniformReservoir;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import javax.management.MBeanServer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 14.09.15.
 */
public class BasicIOStats
implements IOStats {
	//
	protected final String name;
	protected final Clock clock = new ResumableUserTimeClock();
	protected final MetricRegistry metrics = new MetricRegistry();
	protected final MBeanServer mBeanServer;
	protected final JmxReporter jmxReporter;
	protected AtomicLong reqDurationSum = new AtomicLong(0);
	//
	protected Meter throughPutSucc, throughPutFail, reqBytes;
	protected Histogram reqDuration, respLatency;
	//
	public BasicIOStats(final String name, final int serveJmxPort) {
		this.name = name;
		if(serveJmxPort > 0) {
			mBeanServer = ServiceUtils.getMBeanServer(serveJmxPort);
			jmxReporter = JmxReporter.forRegistry(metrics).registerWith(mBeanServer).build();
			jmxReporter.start();
		} else {
			mBeanServer = null;
			jmxReporter = null;
		}
	}
	//
	@Override
	public void start() {
		// init load exec time dependent metrics
		throughPutSucc = metrics.register(
			MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_TP),
			new Meter(clock)
		);
		throughPutFail = metrics.register(
			MetricRegistry.name(name, METRIC_NAME_FAIL),
			new Meter(clock)
		);
		reqBytes = metrics.register(
			MetricRegistry.name(name, METRIC_NAME_REQ, METRIC_NAME_BW),
			new Meter(clock)
		);
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
	public void markSucc(final long size, final int duration, final int latency) {
		throughPutSucc.mark();
		reqBytes.mark(size);
		reqDuration.update(duration);
		reqDurationSum.addAndGet(duration);
		respLatency.update(latency);
	}
	//
	@Override
	public void markSucc(
		final long count, final long bytes, final long durationValues[], final long latencyValues[]
	) {
		throughPutSucc.mark(count);
		reqBytes.mark(bytes);
		for(final long duration : durationValues) {
			reqDuration.update(duration);
			reqDurationSum.addAndGet(duration);
		}
		for(final long latency : latencyValues) {
			respLatency.update(latency);
		}
	}
	//
	@Override
	public void markFail() {
		throughPutFail.mark();
	}
	//
	@Override
	public void markFail(final long count) {
		throughPutFail.mark(count);
	}
	//
	@Override
	public void close()
	throws IOException {
		jmxReporter.stop();
	}
	//
	@Override
	public Snapshot getSnapshot() {
		return new BasicSnapshot(
			throughPutSucc.getCount(), throughPutSucc.getMeanRate(), throughPutSucc.getOneMinuteRate(),
			throughPutFail.getCount(), throughPutFail.getMeanRate(), throughPutFail.getOneMinuteRate(),
			reqBytes.getCount(), reqBytes.getMeanRate(), reqBytes.getOneMinuteRate(),
			reqDuration.getSnapshot(), respLatency.getSnapshot(),
			reqDurationSum.get()
		);
	}
	//
	public static class BasicSnapshot
	implements Snapshot {
		//
		private final long countSucc;
		private final double succRateMean;
		private final double succRateLast;
		private final long countFail;
		private final double failRateMean;
		private final double failRateLast;
		private final long countByte;
		private final double byteRateMean;
		private final double byteRateLast;
		private final com.codahale.metrics.Snapshot snapshotDur;
		private final com.codahale.metrics.Snapshot snapshotLat;
		private final long sumDur;
		//
		public BasicSnapshot(
			final long countSucc, final double succRateMean, final double succRateLast,
			final long countFail, final double failRateMean, final double failRateLast,
			final long countByte, final double byteRateMean, final double byteRateLast,
			final com.codahale.metrics.Snapshot snapshotDur,
			final com.codahale.metrics.Snapshot snapshotLat,
		    final long sumDur
		) {
			this.countSucc = countSucc;
			this.succRateMean = succRateMean;
			this.succRateLast = succRateLast;
			this.countFail = countFail;
			this.failRateMean = failRateMean;
			this.failRateLast = failRateLast;
			this.countByte = countByte;
			this.byteRateMean = byteRateMean;
			this.byteRateLast = byteRateLast;
			this.snapshotDur = snapshotDur;
			this.snapshotLat = snapshotLat;
			this.sumDur = sumDur;
		}
		//
		@Override
		public long getSuccCount() {
			return countSucc;
		}
		//
		@Override
		public double getSuccRatio() {
			return ((double) countSucc) / (countSucc + countFail);
		}
		//
		@Override
		public double getSuccRateMean() {
			return succRateMean;
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
		public double getFailRatio() {
			return ((double) countFail) / (countSucc + countFail);
		}
		//
		@Override
		public double getFailRateMean() {
			return failRateMean;
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
			return byteRateMean;
		}
		//
		@Override
		public double getByteRateLast() {
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
				succRateMean, succRateLast, byteRateMean / MIB, byteRateLast / MIB
			);
		}
	}
}
