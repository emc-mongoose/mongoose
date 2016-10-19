package com.emc.mongoose.core.api.load.model.metrics;
//
import java.io.Closeable;
import java.io.Serializable;
/**
 Created by kurila on 14.09.15.
 */
public interface IoStats
extends Closeable {
	//
	int MIB = 0x100000;
	//
	String METRIC_NAME_SUCC = "succ";
	String METRIC_NAME_FAIL = "fail";
	String METRIC_NAME_BYTE = "byte";
	String METRIC_NAME_TP = "TP";
	String METRIC_NAME_BW = "BW";
	String METRIC_NAME_DUR = "dur";
	String METRIC_NAME_LAT = "lat";
	String METRIC_NAME_COUNT = "count";
	String METRIC_NAME_RATE = "rate";
	String METRIC_NAME_MEAN = "mean";
	String METRIC_NAME_LAST = "last";
	//
	String MSG_FMT_PAIR = "%.3f/%.3f";
	String MSG_FMT_COUNT = "n=(%d/%d); ";
	String MSG_FMT_SIZE = "size=(%s); ";
	String MSG_FMT_TIME = "t[s]=(%s/%s); ";
	String MSG_FMT_TP= "TP[op/s]=(%s/%s); ";
	String MSG_FMT_BW = "BW[MB/s]=(%s/%s); ";
	String MSG_FMT_DUR = "dur[us]=(%s); ";
	String MSG_FMT_LAT = "lat[us]=(%s)";
	String MSG_FMT_METRICS = MSG_FMT_COUNT + MSG_FMT_SIZE + MSG_FMT_TIME + MSG_FMT_TP + MSG_FMT_BW +
		MSG_FMT_DUR + MSG_FMT_LAT;
	//
	void start();
	boolean isStarted();
	//
	void markSucc(final long size, final int duration, final int latency);
	void markSucc(
		final long count, final long bytes, final long durationValues[], final long latencyValues[]
	);
	//
	void markFail();
	void markFail(final long count);
	//
	void markElapsedTime(final long usec);
	//
	Snapshot getSnapshot();
	//
	interface Snapshot
	extends Serializable {
		//
		long getStartTimeMilliSec();
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
		//
		String toCountsString();
		String toDurString();
		String toDurSummaryString();
		String toLatString();
		String toLatSummaryString();
		String toSuccRatesString();
		String toByteRatesString();
		String toSummaryString();
	}
}
