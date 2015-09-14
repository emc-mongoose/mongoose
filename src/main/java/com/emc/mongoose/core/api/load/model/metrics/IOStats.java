package com.emc.mongoose.core.api.load.model.metrics;
//
import java.io.Closeable;
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
		METRIC_NAME_SUBM = "subm",
		METRIC_NAME_REJ = "rej",
		METRIC_NAME_REQ = "req",
		METRIC_NAME_TP = "TP",
		METRIC_NAME_BW = "BW",
		METRIC_NAME_DUR = "dur",
		METRIC_NAME_LAT = "lat",
		NAME_SEP = "@";
	//
	String MSG_FMT_METRICS = "count=(%d/%s); dur[us]=(%d/%d/%d/%d); lat[us]=(%d/%d/%d/%d); " +
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
	Snapshot getSnapshot();
	//
	interface Snapshot
	extends Serializable {
		//
		long getSuccCount();
		double getSuccRatio();
		double getSuccRateMean();
		double getSuccRateLast();
		//
		long getFailCount();
		double getFailRatio();
		double getFailRateMean();
		double getFailRateLast();
		//
		long getByteCount();
		double getByteRateMean();
		double getByteRateLast();
		//
		double getDurationMean();
		double getDurationMin();
		double getDurationStdDev();
		double getDurationMax();
		long getDurationSum();
		long[] getDurationValues();
		//
		double getLatencyMean();
		double getLatencyMin();
		double getLatencyStdDev();
		double getLatencyMax();
		long[] getLatencyValues();
	}
}
