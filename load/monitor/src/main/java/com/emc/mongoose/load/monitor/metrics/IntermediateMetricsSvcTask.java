package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.load.LoadMonitor;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import static java.lang.System.nanoTime;

/**
 Created by andrey on 31.03.17.
 */
public class IntermediateMetricsSvcTask
extends ReentrantLock
implements Runnable {

	private final long metricsPeriodNanoSec;
	private final Int2ObjectMap<IoStats> ioStats;
	private final Int2ObjectMap<IoStats.Snapshot> lastStats;
	private final Int2ObjectMap<IoStats> medIoStats;
	private final Int2ObjectMap<IoStats.Snapshot> lastMedStats;
	private final String stepName;
	private final Int2IntMap driversCountMap;
	private final Int2IntMap concurrencyMap;
	private final boolean fileOutputFlag;

	private volatile long prevNanoTimeStamp;
	private volatile long nextNanoTimeStamp;

	private final LoadMonitor loadMonitor;
	private final int activeTasksThreshold;

	private volatile boolean inStateFlag;

	public IntermediateMetricsSvcTask(
		final LoadMonitor loadMonitor, final String stepName, final int metricsPeriodSec,
		final boolean fileOutputFlag, final Int2IntMap driversCountMap,
		final Int2IntMap concurrencyMap, final Int2ObjectMap<IoStats> ioStats,
		final Int2ObjectMap<IoStats.Snapshot> lastStats, final Int2ObjectMap<IoStats> medIoStats,
		final Int2ObjectMap<IoStats.Snapshot> lastMedStats, final int activeTasksThreshold
	) {
		this.stepName = stepName;
		this.metricsPeriodNanoSec = TimeUnit.SECONDS.toNanos(
			metricsPeriodSec > 0 ? metricsPeriodSec : Long.MAX_VALUE
		);
		this.prevNanoTimeStamp = -1;
		this.fileOutputFlag = fileOutputFlag;
		this.ioStats = ioStats;
		this.lastStats = lastStats;
		this.medIoStats = medIoStats;
		this.lastMedStats = lastMedStats;
		this.concurrencyMap = concurrencyMap;
		this.driversCountMap = driversCountMap;

		this.loadMonitor = loadMonitor;
		this.activeTasksThreshold = activeTasksThreshold;
	}

	@Override
	public final void run() {
		if(tryLock()) {
			try {
				nextNanoTimeStamp = nanoTime();
				if(LoadMonitor.STATS_REFRESH_PERIOD_NANOS > nextNanoTimeStamp - prevNanoTimeStamp) {
					return;
				}
				IoStats.refreshLastStats(ioStats, lastStats);
				if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
					IoStats.outputLastStats(
						lastStats, driversCountMap, concurrencyMap, stepName, fileOutputFlag
					);
					prevNanoTimeStamp = nextNanoTimeStamp;
					LockSupport.parkNanos(1);
				}
				
				if(medIoStats != null) {
					if(loadMonitor.getActiveTaskCount() >= activeTasksThreshold) {
						if(!inStateFlag) {
							inStateFlag = true;
						}
					} else if(inStateFlag) {
						try {
							loadMonitor.getSvcTasks().remove(this); // stop
						} catch(final RemoteException ignored) {
						}
					}
					LockSupport.parkNanos(1);
	
					if(inStateFlag) {
						IoStats.refreshLastStats(medIoStats, lastMedStats);
						if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
							IoStats.outputLastStats(lastMedStats, driversCountMap, concurrencyMap,
								stepName, fileOutputFlag
							);
							prevNanoTimeStamp = nextNanoTimeStamp;
						}
					}
				}
			} finally {
				unlock();
			}
		}
	}
}
