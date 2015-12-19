package com.emc.mongoose.client.impl.load.metrics.model;
//
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.base.Item;
//
import com.emc.mongoose.core.impl.load.model.metrics.BasicIOStats;
import com.emc.mongoose.core.impl.load.model.metrics.IOStatsBase;
//
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.rmi.ConnectIOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 14.09.15.
 */
public class AggregatedRemoteIOStats<T extends Item, W extends LoadSvc<T>>
extends IOStatsBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Map<String, W> loadSvcMap;
	private final Map<String, Snapshot> loadStatsSnapshotMap;
	private final ExecutorService statsLoader;
	//
	private long
		countSucc = 0,
		countFail = 0,
		countByte = 0,
		sumDurMicroSec = 0,
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
	public AggregatedRemoteIOStats(
		final String name, final int serveJmxPort, final Map<String, W> loadSvcMap
	) {
		super(name, serveJmxPort);
		this.loadSvcMap = loadSvcMap;
		this.loadStatsSnapshotMap = new ConcurrentHashMap<>(loadSvcMap.size());
		statsLoader = Executors.newFixedThreadPool(
			loadSvcMap.size(), new GroupThreadFactory("statsLoader<" + name + ">", true)
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
	private final static int COUNT_LIMIT_RETRIES = 100;
	//
	private final class LoadIOStatsSnapshotTask
	implements Runnable {
		//
		private final String loadSvcAddr;
		//
		public LoadIOStatsSnapshotTask(final String loadSvcAddr) {
			this.loadSvcAddr = loadSvcAddr;
		}
		//
		@Override
		public void run() {
			Snapshot loadSvcStatsSnapshot;
			final LoadSvc loadSvc = loadSvcMap.get(loadSvcAddr);
			final Thread currThread = Thread.currentThread();
			currThread.setName(currThread.getName() + "@" + loadSvcAddr);
			int retryCount = 0;
			while(!currThread.isInterrupted()) {
				try {
					loadSvcStatsSnapshot = loadSvc.getStatsSnapshot();
					retryCount = 0; // reset
					if(loadSvcStatsSnapshot != null) {
						if(LOG.isTraceEnabled(Markers.MSG)) {
							LOG.trace(
								Markers.MSG, "Got metrics snapshot from {}: {}",
								loadSvcAddr, loadSvcStatsSnapshot
							);
						}
						loadStatsSnapshotMap.put(loadSvcAddr, loadSvcStatsSnapshot);
					} else {
						LOG.warn(
							Markers.ERR, "Got null metrics snapshot from the load server @ {}",
							loadSvcAddr
						);
					}
					LockSupport.parkNanos(1);
					Thread.yield();
				} catch(final NoSuchObjectException | ConnectIOException e) {
					if(retryCount < COUNT_LIMIT_RETRIES) {
						retryCount ++;
						LogUtil.exception(
							LOG, Level.DEBUG, e,
							"Failed to fetch the metrics snapshot from {} {} times",
							loadSvcAddr, retryCount
						);
					} else {
						LogUtil.exception(
							LOG, Level.ERROR, e,
							"Failed to fetch the metrics from {} {} times, stopping the task",
							loadSvcAddr, retryCount
						);
						break;
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e,
						"Failed to fetch the metrics snapshot from {}", loadSvcAddr
					);
				}
			}
		}
	}
	//
	@Override
	public final void start() {
		for(final String addr : loadSvcMap.keySet()) {
			statsLoader.submit(new LoadIOStatsSnapshotTask(addr));
		}
		statsLoader.shutdown();
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
					succRateMean = 0;
					succRateLast = 0;
					failRateMean = 0;
					failRateLast = 0;
					byteRateMean = 0;
					byteRateLast = 0;
					//
					Snapshot loadStatsSnapshot;
					for(final String addr : loadStatsSnapshotMap.keySet()) {
						loadStatsSnapshot = loadStatsSnapshotMap.get(addr);
						if(loadStatsSnapshot != null) {
							applyNextServerSnapshot(loadStatsSnapshot);
						} else {
							LOG.warn(
								Markers.ERR, "No load stats snapshot is available for {}", addr
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
		return new BasicIOStats.BasicSnapshot(
			countSucc, succRateLast, countFail, failRateLast, countByte, byteRateLast,
			prevElapsedTimeMicroSec + currElapsedTime, sumDurMicroSec,
			reqDuration.getSnapshot(), respLatency.getSnapshot()
		);
	}
	//
	private void applyNextServerSnapshot(final Snapshot loadStatsSnapshot) {
		countSucc += loadStatsSnapshot.getSuccCount();
		countFail += loadStatsSnapshot.getFailCount();
		countByte += loadStatsSnapshot.getByteCount();
		sumDurMicroSec += loadStatsSnapshot.getDurationSum();
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
