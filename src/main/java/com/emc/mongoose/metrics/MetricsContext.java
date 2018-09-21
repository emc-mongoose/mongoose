package com.emc.mongoose.metrics;

import com.emc.mongoose.item.op.OpType;

import com.github.akurilov.commons.system.SizeInBytes;

import java.io.Closeable;

/**
 Created by andrey on 14.07.16.
 */
public interface MetricsContext<S extends MetricsSnapshot>
extends Closeable, Comparable<MetricsContext<S>> {

	int DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS = 10;
	int DEFAULT_RESERVOIR_SIZE = 0x1_000;

	// these are useful as labels/tags
	String id();

	OpType opType();

	int concurrencyLimit();

	SizeInBytes itemDataSize();

	// metrics accounting methods

	void markSucc(final long bytes, final long duration, final long latency);

	void markPartSucc(final long bytes, final long duration, final long latency);

	void markSucc(final long count, final long bytes, final long durationValues[], final long latencyValues[]);

	void markPartSucc(final long bytes, final long durationValues[], final long latencyValues[]);

	void markFail();

	void markFail(final long count);

	void markElapsedTime(final long millis);

	// state control methods below

	void start();

	boolean isStarted();

	long startTimeStamp();

	void refreshLastSnapshot();

	S lastSnapshot();

	long transferSizeSum();

	// threshold-related accounting methods below

	int concurrencyThreshold();

	boolean thresholdStateEntered();
	
	void enterThresholdState()
	throws IllegalStateException;

	boolean thresholdStateExited();

	MetricsContext thresholdMetrics();

	void exitThresholdState()
	throws IllegalStateException;

	// output configuration methods below

	boolean stdOutColorEnabled();

	boolean avgPersistEnabled();

	boolean sumPersistEnabled();

	boolean perfDbResultsFileEnabled();

	long outputPeriodMillis();

	long lastOutputTs();

	void lastOutputTs(final long ts);
}
