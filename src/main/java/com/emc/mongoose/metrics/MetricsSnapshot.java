package com.emc.mongoose.metrics;

import java.io.Serializable;

public interface MetricsSnapshot
extends Serializable {

	long succCount();

	long failCount();

	long byteCount();

	/** @return value in milliseconds */
	long startTimeMillis();
	//
	double succRateMean();
	double succRateLast();
	//
	double failRateMean();
	double failRateLast();
	//
	double byteRateMean();
	double byteRateLast();

	/** @return value in milliseconds */
	long elapsedTimeMillis();

	int actualConcurrencyLast();

	double actualConcurrencyMean();

	/** @return value in microseconds */
	long durationSum();

	/** @return value in microseconds */
	long latencySum();

	/** @return value in microseconds */
	long durationMin();

	/** @return value in microseconds */
	long durationLoQ();

	/** @return value in microseconds */
	long durationMed();

	/** @return value in microseconds */
	long durationHiQ();

	/** @return value in microseconds */
	long durationMax();

	/** @return value in microseconds */
	double durationMean();

	/** @return values in microseconds */
	long[] durationValues();

	/** @return value in microseconds */
	long latencyMin();

	/** @return value in microseconds */
	long latencyLoQ();

	/** @return value in microseconds */
	long latencyMed();

	/** @return value in microseconds */
	long latencyHiQ();

	/** @return value in microseconds */
	long latencyMax();

	/** @return value in microseconds */
	double latencyMean();

	/** @return values in microseconds */
	long[] latencyValues();
}
