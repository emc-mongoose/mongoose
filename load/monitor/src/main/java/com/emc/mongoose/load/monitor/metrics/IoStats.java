package com.emc.mongoose.load.monitor.metrics;

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

		/** @return microseconds */
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
		//
		long getElapsedTime();
		long getDurationSum();
		long getLatencySum();
		//
		long getDurationMin();
		long getDurationLoQ();
		long getDurationMed();
		long getDurationHiQ();
		long getDurationMax();
		long[] getDurationValues();
		double getDurationAvg();
		//
		long getLatencyMin();
		long getLatencyLoQ();
		long getLatencyMed();
		long getLatencyHiQ();
		long getLatencyMax();
		long[] getLatencyValues();
		double getLatencyAvg();
		/*
		String toCountsString();
		String toDurString();
		String toDurSummaryString();
		String toLatString();
		String toLatSummaryString();
		String toSummaryString();*/
	}
}
