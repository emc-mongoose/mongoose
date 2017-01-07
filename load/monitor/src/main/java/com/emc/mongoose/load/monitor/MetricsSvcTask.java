package com.emc.mongoose.load.monitor;

import com.emc.mongoose.load.monitor.metrics.IoStats;
import com.emc.mongoose.load.monitor.metrics.MetricsCsvLogMessage;
import com.emc.mongoose.load.monitor.metrics.MetricsStdoutLogMessage;
import com.emc.mongoose.ui.log.Markers;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 15.12.16.
 */
public final class MetricsSvcTask
implements Runnable {

	private static final Logger LOG = LogManager.getLogger();

	private final long metricsPeriodNanoSec;
	private final Int2ObjectMap<IoStats> ioStats;
	private final Int2ObjectMap<IoStats.Snapshot> lastStats;
	private final String jobName;
	private final Int2IntMap driversCountMap;
	private final Int2IntMap concurrencyMap;
	private final boolean fileOutputFlag;

	private long prevNanoTimeStamp;

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
		final Thread currThread = Thread.currentThread();
		currThread.setName(jobName);
		long nextNanoTimeStamp;
		while(!currThread.isInterrupted()) {
			refreshStats();
			nextNanoTimeStamp = System.nanoTime();
			if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
				outputCurrentMetrics();
				prevNanoTimeStamp = nextNanoTimeStamp;
			}
			try {
				Thread.sleep(1);
			} catch(final InterruptedException e) {
				break;
			}
		}
	}

	private void refreshStats() {
		for(final int nextIoTypeCode : ioStats.keySet()) {
			lastStats.put(nextIoTypeCode, ioStats.get(nextIoTypeCode).getSnapshot());
		}
	}

	private void outputCurrentMetrics() {
		LOG.info(
			Markers.METRICS_STDOUT,
			new MetricsStdoutLogMessage(jobName, lastStats, concurrencyMap, driversCountMap)
		);
		if(!fileOutputFlag) {
			LOG.info(
				Markers.METRICS_FILE,
				new MetricsCsvLogMessage(lastStats, concurrencyMap, driversCountMap)
			);
		}
	}
}