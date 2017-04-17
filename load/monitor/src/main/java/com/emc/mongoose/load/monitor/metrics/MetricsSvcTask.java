package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.load.monitor.metrics.IoStats;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import static java.lang.System.nanoTime;

/**
 Created by andrey on 15.12.16.
 */
public final class MetricsSvcTask
extends ReentrantLock
implements Runnable {

	private final long metricsPeriodNanoSec;
	private final Int2ObjectMap<IoStats> ioStats;
	private final Int2ObjectMap<IoStats.Snapshot> lastStats;
	private final String jobName;
	private final Int2IntMap driversCountMap;
	private final Int2IntMap concurrencyMap;
	private final boolean fileOutputFlag;

	private volatile long prevNanoTimeStamp;
	private volatile long nextNanoTimeStamp;

	public MetricsSvcTask(
		final String jobName, final int metricsPeriodSec, final boolean fileOutputFlag,
		final Int2ObjectMap<IoStats> ioStats, Int2ObjectMap<IoStats.Snapshot> lastStats,
		final Int2IntMap driversCountMap, final Int2IntMap concurrencyMap
	) {
		this.jobName = jobName;
		this.metricsPeriodNanoSec = TimeUnit.SECONDS.toNanos(
			metricsPeriodSec > 0 ? metricsPeriodSec : Long.MAX_VALUE
		);
		this.prevNanoTimeStamp = -1;
		this.fileOutputFlag = fileOutputFlag;
		this.ioStats = ioStats;
		this.lastStats = lastStats;
		this.concurrencyMap = concurrencyMap;
		this.driversCountMap = driversCountMap;
	}
	
	@Override
	public final void run() {
		if(tryLock()) {
			try {
				IoStats.refreshLastStats(ioStats, lastStats);
				LockSupport.parkNanos(1);
				nextNanoTimeStamp = nanoTime();
				if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
					IoStats.outputLastStats(
						lastStats, driversCountMap, concurrencyMap, jobName, fileOutputFlag
					);
					prevNanoTimeStamp = nextNanoTimeStamp;
				}
			} finally {
				unlock();
			}
		}
	}
}
