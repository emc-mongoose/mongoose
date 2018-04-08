package com.emc.mongoose.api.metrics;

import com.github.akurilov.commons.system.SizeInBytes;

import com.emc.mongoose.api.model.io.IoType;

import java.io.Closeable;

/**
 Created by andrey on 14.07.16.
 */
public interface MetricsContext
extends Closeable, Comparable<MetricsContext> {

	int DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS = 10;
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
	int getNodeCount();
	int getConcurrency();
	int getConcurrencyThreshold();
	int getActualConcurrency();
	SizeInBytes getItemDataSize();

	boolean getStdOutColorFlag();
	boolean getAvgPersistFlag();
	boolean getSumPersistFlag();
	boolean getPerfDbResultsFileFlag();
	long getOutputPeriodMillis();
	long getLastOutputTs();
	void setLastOutputTs(final long ts);
	
	void refreshLastSnapshot();

	MetricsSnapshot getLastSnapshot();

	void setMetricsListener(final MetricsListener metricsListener);

	boolean isThresholdStateEntered();
	
	void enterThresholdState()
	throws IllegalStateException;

	boolean isThresholdStateExited();

	MetricsContext getThresholdMetrics();

	void exitThresholdState()
	throws IllegalStateException;
}
