package com.emc.mongoose.common.load.metrics;

import java.io.Serializable;
/**
 Created by andrey on 14.07.16.
 */
public interface IoStats {

	int MIB = 0x100000;

	void start();
	boolean isStarted();

	void markSucc(final long size, final int duration, final int latency);
	void markSucc(
		final long count, final long bytes, final long durationValues[], final long latencyValues[]
	);

	void markFail();
	void markFail(final long count);

	void markElapsedTime(final long usec);

	Snapshot getSnapshot();
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
