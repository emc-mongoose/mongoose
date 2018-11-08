package com.emc.mongoose.metrics.context;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.snapshot.AllMetricsSnapshot;
import com.github.akurilov.commons.system.SizeInBytes;

import java.util.List;

/**
 Created by andrey on 14.07.16.
 */
public interface MetricsContext<S extends AllMetricsSnapshot>
extends AutoCloseable, Comparable<MetricsContext<S>> {

	int DEFAULT_SNAPSHOT_UPDATE_PERIOD_MILLIS = 10;
	int DEFAULT_RESERVOIR_SIZE = 1028;

	// these are useful as labels/tags
	String id();

	OpType opType();

	int concurrencyLimit();

	SizeInBytes itemDataSize();
	// metrics accounting methods

	List<Double> quantileValues();

	void markSucc(final long bytes, final long duration, final long latency);

	void markPartSucc(final long bytes, final long duration, final long latency);

	void markSucc(final long count, final long bytes, final long durationValues[], final long latencyValues[]);

	void markPartSucc(final long bytes, final long durationValues[], final long latencyValues[]);

	void markFail();

	void markFail(final long count);

	void start();

	boolean isStarted();

	long startTimeStamp();

	void refreshLastSnapshot();

	S lastSnapshot();

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

	long elapsedTimeMillis();

	@Override
	void close();
}
