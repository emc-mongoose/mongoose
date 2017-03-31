package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.load.LoadMonitor;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.Set;
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
	private final String jobName;
	private final Int2IntMap driversCountMap;
	private final Int2IntMap concurrencyMap;
	private final boolean fileOutputFlag;

	private volatile long prevNanoTimeStamp;
	private volatile long nextNanoTimeStamp;

	private final LoadMonitor loadMonitor;
	private final Set<Runnable> svcTasks;
	private final int activeTasksThreshold;

	private volatile boolean inStateFlag;

	public IntermediateMetricsSvcTask(
		final String jobName, final int metricsPeriodSec, final boolean fileOutputFlag,
		final Int2ObjectMap<IoStats> ioStats, Int2ObjectMap<IoStats.Snapshot> lastStats,
		final Int2IntMap driversCountMap, final Int2IntMap concurrencyMap,
		final LoadMonitor loadMonitor, final Set<Runnable> svcTasks, final int activeTasksThreshold
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

		this.loadMonitor = loadMonitor;
		this.svcTasks = svcTasks;
		this.activeTasksThreshold = activeTasksThreshold;
	}

	@Override
	public final void run() {
		if(tryLock()) {
			try {
				if(loadMonitor.getActiveTaskCount() >= activeTasksThreshold) {
					if(!inStateFlag) {
						inStateFlag = true;
					}
				} else if(inStateFlag) {
					svcTasks.remove(this); // stop
				}

				if(inStateFlag) {
					IoStats.refreshLastStats(ioStats, lastStats);
					LockSupport.parkNanos(1);
					nextNanoTimeStamp = nanoTime();
					if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
						IoStats.outputLastStats(
							lastStats, driversCountMap, concurrencyMap, jobName, fileOutputFlag
						);
						prevNanoTimeStamp = nextNanoTimeStamp;
					}
				}
			} finally {
				unlock();
			}
		}
	}
}
