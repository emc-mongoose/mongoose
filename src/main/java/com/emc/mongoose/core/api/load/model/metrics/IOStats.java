package com.emc.mongoose.core.api.load.model.metrics;
//
import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
/**
 Created by kurila on 14.09.15.
 */
public interface IOStats
extends Closeable {
	//
	int MIB = 0x100000;
	//
	String
		METRIC_NAME_SUCC = "succ",
		METRIC_NAME_FAIL = "fail",
		METRIC_NAME_BYTE = "byte",
		METRIC_NAME_SUBM = "subm",
		METRIC_NAME_REJ = "rej",
		METRIC_NAME_REQ = "req",
		METRIC_NAME_TP = "TP",
		METRIC_NAME_BW = "BW",
		METRIC_NAME_DUR = "dur",
		METRIC_NAME_LAT = "lat",
		//
		METRIC_NAME_COUNT = "count",
		METRIC_NAME_RATE = "rate",
		METRIC_NAME_MEAN = "mean",
		METRIC_NAME_LAST = "last",
		METRIC_NAME_MIN = "min",
		METRIC_NAME_STDDEV = "stdDev",
		METRIC_NAME_MAX = "max",
		//
		NAME_SEP = "@",
		//
		MSG_FMT_METRICS = "count=(%d/%s); dur[us]=(%d/%d/%d/%d/%d); lat[us]=(%d/%d/%d/%d/%d); " +
			"TP[s^-1]=(%.3f/%.3f); BW[MB*s^-1]=(%.3f/%.3f)";
	//
	void start();
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
		//
		double getDurationMin();
		double getDurationLoQ();
		double getDurationMed();
		double getDurationHiQ();
		double getDurationMax();
		long[] getDurationValues();
		//
		double getLatencyMin();
		double getLatencyLoQ();
		double getLatencyMed();
		double getLatencyHiQ();
		double getLatencyMax();
		long[] getLatencyValues();
	}
}
