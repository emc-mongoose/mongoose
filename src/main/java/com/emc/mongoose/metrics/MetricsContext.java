package com.emc.mongoose.metrics;

import com.emc.mongoose.item.op.OpType;

import com.github.akurilov.commons.system.SizeInBytes;

import java.io.Closeable;

/**
 Created by andrey on 14.07.16.
 */
public interface MetricsContext
extends Closeable, Comparable<MetricsContext> {

	int DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS = 10;
	int DEFAULT_RESERVOIR_SIZE = 0x10_00;

	// these are useful as labels/tags
	String stepId();
	OpType ioType();
	int nodeCount();
	int concurrencyLimit();
	SizeInBytes itemDataSize();


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
	
	int concurrencyThreshold();
	int actualConcurrency();

	boolean stdOutColorEnabled();
	boolean avgPersistEnabled();
	boolean sumPersistEnabled();
	boolean perfDbResultsFileEnabled();
	long outputPeriodMillis();
	long lastOutputTs();
	void lastOutputTs(final long ts);
	
	void refreshLastSnapshot();

	MetricsSnapshot lastSnapshot();

	void metricsListener(final MetricsListener metricsListener);

	boolean thresholdStateEntered();
	
	void enterThresholdState()
	throws IllegalStateException;

	boolean thresholdStateExited();

	MetricsContext thresholdMetrics();

	void exitThresholdState()
	throws IllegalStateException;
}
