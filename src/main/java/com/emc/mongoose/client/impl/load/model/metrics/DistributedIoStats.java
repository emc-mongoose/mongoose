package com.emc.mongoose.client.impl.load.model.metrics;
//
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.client.api.load.tasks.LoadStatsSnapshotTask;
import com.emc.mongoose.client.impl.load.tasks.BasicLoadStatsSnapshotTask;
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.base.Item;
//
import com.emc.mongoose.core.impl.load.model.metrics.BasicIoStats;
import com.emc.mongoose.core.impl.load.model.metrics.IoStatsBase;
//
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 14.09.15.
 */
public class DistributedIoStats<T extends Item, W extends LoadSvc<T>>
extends IoStatsBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final Map<String, W> loadSvcMap;
	protected final ExecutorService statsLoader;
	protected final Map<String, LoadStatsSnapshotTask> loadStatsSnapshotMap;
	//
	private long
		countSucc = 0,
		countFail = 0,
		countByte = 0,
		sumDurMicroSec = 0,
		sumLatMicroSec = 0,
		durationValues[],
		latencyValues[];
	private double
		succRateMean = 0,
		succRateLast = 0,
		failRateMean = 0,
		failRateLast = 0,
		byteRateMean = 0,
		byteRateLast = 0;
	private final Lock
		lock = new ReentrantLock();
	//
	public DistributedIoStats(
		final String name, final boolean serveJmxFlag, final Map<String, W> loadSvcMap
	) {
		super(name, serveJmxFlag);
		this.loadSvcMap = loadSvcMap;
		this.loadStatsSnapshotMap = new ConcurrentHashMap<>(loadSvcMap.size());
		statsLoader = Executors.newFixedThreadPool(
			loadSvcMap.size(), new NamingThreadFactory("statsLoader<" + name + ">", true)
		);
		//
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_SUCC, METRIC_NAME_COUNT),
			new Gauge<Long>() {
				@Override
				public final Long getValue() {
					long x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = countSucc;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_SUCC, METRIC_NAME_RATE, METRIC_NAME_MEAN),
			new Gauge<Double>() {
				@Override
				public final Double getValue() {
					double x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = succRateMean;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_SUCC, METRIC_NAME_RATE, METRIC_NAME_LAST),
			new Gauge<Double>() {
				@Override
				public final Double getValue() {
					double x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = succRateLast;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_FAIL, METRIC_NAME_COUNT),
			new Gauge<Long>() {
				@Override
				public final Long getValue() {
					long x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = countFail;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_FAIL, METRIC_NAME_RATE, METRIC_NAME_MEAN),
			new Gauge<Double>() {
				@Override
				public final Double getValue() {
					double x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = failRateMean;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_FAIL, METRIC_NAME_RATE, METRIC_NAME_LAST),
			new Gauge<Double>() {
				@Override
				public final Double getValue() {
					double x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = failRateLast;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_BYTE, METRIC_NAME_COUNT),
			new Gauge<Long>() {
				@Override
				public final Long getValue() {
					long x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = countByte;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_BYTE, METRIC_NAME_RATE, METRIC_NAME_MEAN),
			new Gauge<Double>() {
				@Override
				public final Double getValue() {
					double x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = byteRateMean;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
		metrics.register(
			MetricRegistry.name(name, METRIC_NAME_BYTE, METRIC_NAME_RATE, METRIC_NAME_LAST),
			new Gauge<Double>() {
				@Override
				public final Double getValue() {
					double x = 0;
					try {
						if(lock.tryLock(1, TimeUnit.SECONDS)) {
							x = byteRateLast;
							lock.unlock();
						} else {
							throw new IllegalStateException("Failed to acquire the lock in 1 sec");
						}
					} catch(final InterruptedException | IllegalStateException e) {
						LogUtil.exception(LOG, Level.WARN, e, "I/O stats is locked by aggregation");
					}
					return x;
				}
			}
		);
	}
	//
	@Override
	public void start() {
		if(!statsLoader.isShutdown()) {
			LoadStatsSnapshotTask nextLoadStatsSnapshotTask;
			for(final String addr : loadSvcMap.keySet()) {
				nextLoadStatsSnapshotTask = new BasicLoadStatsSnapshotTask(
					loadSvcMap.get(addr), addr
				);
				loadStatsSnapshotMap.put(addr, nextLoadStatsSnapshotTask);
				statsLoader.submit(nextLoadStatsSnapshotTask);
			}
			statsLoader.shutdown();
		}
		super.start();
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
		try {
			if(lock.tryLock(1, TimeUnit.SECONDS)) {
				try {
					countSucc = 0;
					countFail = 0;
					countByte = 0;
					sumDurMicroSec = 0;
					sumLatMicroSec = 0;
					succRateMean = 0;
					succRateLast = 0;
					failRateMean = 0;
					failRateLast = 0;
					byteRateMean = 0;
					byteRateLast = 0;
					//
					Snapshot loadStatsSnapshot;
					for(final String addr : loadStatsSnapshotMap.keySet()) {
						loadStatsSnapshot = loadStatsSnapshotMap.get(addr).getLastStatsSnapshot();
						if(loadStatsSnapshot != null) {
							applyNextServerSnapshot(loadStatsSnapshot);
						} else {
							LOG.debug(
								Markers.ERR, "No load stats snapshot is available for {} yet", addr
							);
						}
					}
				} finally {
					lock.unlock();
				}
			} else {
				throw new IllegalStateException("Failed to acquire the lock in 1 sec");
			}
		} catch(final InterruptedException | IllegalStateException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Metrics aggregation failure");
		}
		//
		final long currElapsedTime = tsStartMicroSec > 0 ?
			TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) - tsStartMicroSec : 0;
		return new BasicIoStats.BasicSnapshot(
			countSucc, succRateLast, countFail, failRateLast, countByte, byteRateLast,
			prevElapsedTimeMicroSec + currElapsedTime, sumDurMicroSec, sumLatMicroSec,
			reqDuration.getSnapshot(), respLatency.getSnapshot()
		);
	}
	//
	private void applyNextServerSnapshot(final Snapshot loadStatsSnapshot) {
		countSucc += loadStatsSnapshot.getSuccCount();
		countFail += loadStatsSnapshot.getFailCount();
		countByte += loadStatsSnapshot.getByteCount();
		sumDurMicroSec += loadStatsSnapshot.getDurationSum();
		sumLatMicroSec += loadStatsSnapshot.getLatencySum();
		succRateMean += loadStatsSnapshot.getSuccRateMean();
		succRateLast += loadStatsSnapshot.getSuccRateLast();
		failRateMean += loadStatsSnapshot.getFailRateMean();
		failRateLast += loadStatsSnapshot.getFailRateLast();
		byteRateMean += loadStatsSnapshot.getByteRateMean();
		byteRateLast += loadStatsSnapshot.getByteRateLast();
		durationValues = loadStatsSnapshot.getDurationValues();
		if(durationValues != null) {
			for(final long duration : durationValues) {
				reqDuration.update(duration);
			}
		} else {
			LOG.warn(Markers.ERR, "No duration values are available in the fetched snapshot");
		}
		latencyValues = loadStatsSnapshot.getLatencyValues();
		if(latencyValues != null) {
			for(final long latency : latencyValues) {
				respLatency.update(latency);
			}
		} else {
			LOG.warn(Markers.ERR, "No latency values are available in the fetched snapshot");
		}
	}
	//
	@Override
	public final void close()
	throws IOException {
		super.close();
		statsLoader.shutdownNow();
	}
}
