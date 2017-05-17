package com.emc.mongoose.load.controller.metrics;

import com.emc.mongoose.ui.log.Loggers;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.io.Closeable;
import java.io.Serializable;
/**
 Created by andrey on 14.07.16.
 */
public interface IoStats
extends Closeable {
	
	String MSG_FMT_PAIR = "%.3f/%.3f";
	// count, time, TP, size, BW, duration, latency
	String MSG_FMT_COUNT = "count=(%d/%d); ";
	String MSG_FMT_TIME = "time[s]=(%s/%s); ";
	String MSG_FMT_TP= "TP[op/s]=(" + MSG_FMT_PAIR + "); ";
	String MSG_FMT_SIZE = "size=(%s); ";
	String MSG_FMT_BW = "BW[MB/s]=(" + MSG_FMT_PAIR + "); ";
	String MSG_FMT_DUR = "duration[us]=(%s); ";
	String MSG_FMT_LAT = "latency[us]=(%s)";
	String MSG_FMT_METRICS = MSG_FMT_COUNT + MSG_FMT_TIME + MSG_FMT_TP + MSG_FMT_SIZE + MSG_FMT_BW +
		MSG_FMT_DUR + MSG_FMT_LAT;

	void start();
	boolean isStarted();

	void markSucc(final long size, final long duration, final long latency);
	void markPartSucc(final long size, final long duration, final long latency);
	void markSucc(
		final long count, final long bytes, final long durationValues[], final long latencyValues[]
	);
	void markPartSucc(final long bytes, final long durationValues[], final long latencyValues[]);

	void markFail();
	void markFail(final long count);

	void markElapsedTime(final long usec);

	Snapshot getSnapshot();
	
	interface Snapshot
	extends Serializable {

		/** @return value in milliseconds */
		long getStartTime();
		//
		long getSuccCount();
		double getSuccRateMean();
		double getSuccRateLast();
		//
		long getFailCount();
		double getFailRateMean();
		double getFailRateLast();
		//
		long getByteCount();
		double getByteRateMean();
		double getByteRateLast();
		
		/** @return value in milliseconds */
		long getElapsedTime();

		/** @return value in microseconds */
		long getDurationSum();
		long getLatencySum();
		//
		long getDurationMin();
		long getDurationLoQ();
		long getDurationMed();
		long getDurationHiQ();
		long getDurationMax();
		long[] getDurationValues();
		double getDurationMean();
		//
		long getLatencyMin();
		long getLatencyLoQ();
		long getLatencyMed();
		long getLatencyHiQ();
		long getLatencyMax();
		long[] getLatencyValues();
		double getLatencyMean();
	}
	
	static void refreshLastStats(
		final Int2ObjectMap<IoStats> ioStats, Int2ObjectMap<IoStats.Snapshot> lastStats
	) {
		IoStats ioTypeStats;
		for(final int nextIoTypeCode : ioStats.keySet()) {
			ioTypeStats = ioStats.get(nextIoTypeCode);
			if(ioTypeStats != null) {
				lastStats.put(nextIoTypeCode, ioTypeStats.getSnapshot());
			}
		}
	}
	
	static void outputLastStats(
		Int2ObjectMap<IoStats.Snapshot> lastStats, final Int2IntMap driversCountMap,
		final Int2IntMap concurrencyMap, final String jobName, final boolean volatileOutputFlag
	) {
		Loggers.METRICS_STD_OUT.info(
			new MetricsStdoutLogMessage(jobName, lastStats, concurrencyMap, driversCountMap)
		);
		if(!volatileOutputFlag) {
			Loggers.METRICS_FILE.info(
				new MetricsCsvLogMessage(lastStats, concurrencyMap, driversCountMap)
			);
		}
	}
	
	static void outputLastMedStats(
		Int2ObjectMap<IoStats.Snapshot> lastStats, final Int2IntMap driversCountMap,
		final Int2IntMap concurrencyMap, final String jobName, final boolean volatileOutputFlag
	) {
		if(!volatileOutputFlag) {
			Loggers.METRICS_THRESHOLD_FILE.info(
				new MetricsCsvLogMessage(lastStats, concurrencyMap, driversCountMap)
			);
		}
	}
}
