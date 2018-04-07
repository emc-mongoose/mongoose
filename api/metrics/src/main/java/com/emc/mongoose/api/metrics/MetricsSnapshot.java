package com.emc.mongoose.api.metrics;

import java.io.Serializable;

public interface MetricsSnapshot
extends Serializable {

	long getSuccCount();

	long getFailCount();

	long getByteCount();

	/** @return value in milliseconds */
	long getStartTimeMillis();
	//
	double getSuccRateMean();
	double getSuccRateLast();
	//
	double getFailRateMean();
	double getFailRateLast();
	//
	double getByteRateMean();
	double getByteRateLast();

	/** @return value in milliseconds */
	long getElapsedTimeMillis();

	int getActualConcurrencyLast();

	double getActualConcurrencyMean();

	/** @return value in microseconds */
	long getDurationSum();

	/** @return value in microseconds */
	long getLatencySum();

	/** @return value in microseconds */
	long getDurationMin();

	/** @return value in microseconds */
	long getDurationLoQ();

	/** @return value in microseconds */
	long getDurationMed();

	/** @return value in microseconds */
	long getDurationHiQ();

	/** @return value in microseconds */
	long getDurationMax();

	/** @return value in microseconds */
	double getDurationMean();

	/** @return values in microseconds */
	long[] getDurationValues();

	/** @return value in microseconds */
	long getLatencyMin();

	/** @return value in microseconds */
	long getLatencyLoQ();

	/** @return value in microseconds */
	long getLatencyMed();

	/** @return value in microseconds */
	long getLatencyHiQ();

	/** @return value in microseconds */
	long getLatencyMax();

	/** @return value in microseconds */
	double getLatencyMean();

	/** @return values in microseconds */
	long[] getLatencyValues();
}
