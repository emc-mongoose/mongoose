package com.emc.mongoose.client.impl.load.metrics.model;
//
import com.codahale.metrics.Clock;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.UniformReservoir;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
//
import com.emc.mongoose.core.impl.load.model.metrics.BasicIOStats;
//
import com.emc.mongoose.core.impl.load.model.metrics.ResumableUserTimeClock;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.MBeanServer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.09.15.
 */
public class AggregatedRemoteIOStats
implements IOStats {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final String name;
	protected final MetricRegistry metrics = new MetricRegistry();
	protected final MBeanServer mBeanServer;
	protected final JmxReporter jmxReporter;
	private final int periodSec;
	private final Map<String, LoadSvc> loadSvcMap;
	private final Map<String, Snapshot> loadStatsSnapshotMap;
	private final ScheduledExecutorService statsLoader;
	//
	private Gauge<>
	private Histogram reqDuration, respLatency;
	//
	public AggregatedRemoteIOStats(
		final String name, final int serveJmxPort, final Map<String, LoadSvc> loadSvcMap,
		final int periodSec
	) {
		this.name = name;
		this.periodSec = periodSec;
		this.loadSvcMap = loadSvcMap;
		this.loadStatsSnapshotMap = new HashMap<>(loadSvcMap.size());
		statsLoader = Executors.newScheduledThreadPool(
			Math.max(loadSvcMap.size(), ThreadUtil.getWorkerCount()),
			new GroupThreadFactory("statsLoader<" + name + ">", true)
		);
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
	public final void start() {
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
		for(final String addr : loadSvcMap.keySet()) {
			statsLoader.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						final LoadSvc loadSvc = loadSvcMap.get(addr);
						final Snapshot loadStatsSnapshot = loadSvc.getStatsSnapshot();
						if(loadStatsSnapshot != null) {
							loadStatsSnapshotMap.put(addr, loadStatsSnapshot);
						} else {
							LOG.warn(
								Markers.ERR,
								"Failed to load the stats snapshot from the load server @ {}", addr
							);
						}
					}
				}, 0, periodSec, TimeUnit.SECONDS
			);
		}
	}
	//
	@Override
	public final void markSucc(
		final long size, final int duration, final int latency
	) {
	}
	//
	@Override
	public final void markSucc(
		final long count, final long bytes, final long durationValues[], final long latencyValues[]
	) {
	}
	//
	@Override
	public final void markFail() {
	}
	//
	@Override
	public final void markFail(final long count) {
	}
	//
	@Override
	public final Snapshot getSnapshot() {
		//
		long
			countSucc = 0,
			countFail = 0,
			countByte = 0,
			sumDur = 0,
			durationValues[],
			latencyValues[];
		double
			succRateMean = 0,
			succRateLast = 0,
			failRateMean = 0,
			failRateLast = 0,
			byteRateMean = 0,
			byteRateLast = 0;
		//
		Snapshot loadStatsSnapshot;
		for(final String addr : loadStatsSnapshotMap.keySet()) {
			loadStatsSnapshot = loadStatsSnapshotMap.get(addr);
			if(loadStatsSnapshot != null) {
				countSucc += loadStatsSnapshot.getSuccCount();
				succRateMean += loadStatsSnapshot.getSuccRateMean();
				succRateLast += loadStatsSnapshot.getSuccRateLast();
				countFail += loadStatsSnapshot.getFailCount();
				sumDur += loadStatsSnapshot.getDurationSum();
				durationValues = loadStatsSnapshot.getDurationValues();
				if(durationValues != null) {
					for(final long duration : durationValues) {
						reqDuration.update(duration);
					}
				} else {
					LOG.warn(Markers.ERR, "No duration values snapshot is available for {}", addr);
				}
				latencyValues = loadStatsSnapshot.getLatencyValues();
				if(latencyValues != null) {
					for(final long latency : latencyValues) {
						respLatency.update(latency);
					}
				} else {
					LOG.warn(Markers.ERR, "No latency values snapshot is available for {}", addr);
				}
			} else {
				LOG.warn(Markers.ERR, "No load stats snapshot is available for {}", addr);
			}
		}
		//
		return new BasicIOStats.BasicSnapshot(
			countSucc, succRateMean, succRateLast,
			countFail, failRateMean, failRateLast,
			countByte, byteRateMean, byteRateLast,
			reqDuration.getSnapshot(), respLatency.getSnapshot(), sumDur
		);
	}
	//
	@Override
	public final void close()
	throws IOException {
		statsLoader.shutdownNow();
	}
}
