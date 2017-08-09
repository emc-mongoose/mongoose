package com.emc.mongoose.api.metrics;

import com.emc.mongoose.api.model.io.IoType;

import java.io.Closeable;
import java.io.Serializable;

/**
 Created by andrey on 14.07.16.
 */
public interface MetricsContext
extends Closeable {

	int DEFAULT_RESERVOIR_SIZE = 0x10_00;
	
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

	void markElapsedTime(final long millis);
	
	String getStepId();
	IoType getIoType();
	int getDriverCount();
	int getConcurrency();
	int getConcurrencyThreshold();
	long getTransferSizeEstimate();

	boolean getStdOutColorFlag();
	boolean getAvgPersistFlag();
	boolean getSumPersistFlag();
	boolean getPerfDbResultsFileFlag();
	long getOutputPeriodMillis();
	long getLastOutputTs();
	void setLastOutputTs(final long ts);
	
	void refreshLastSnapshot();
	Snapshot getLastSnapshot();
	void setMetricsListener(final MetricsListener metricsListener);
	
	boolean isThresholdStateEntered();
	
	void enterThresholdState()
	throws IllegalStateException;

	boolean isThresholdStateExited();

	void exitThresholdState()
	throws IllegalStateException;
	
	MetricsContext getThresholdMetrics();
	
	interface Snapshot
	extends Serializable {

		/** @return value in milliseconds */
		long getStartTimeMillis();
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
		long getElapsedTimeMillis();

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
	}
}
