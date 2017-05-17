package com.emc.mongoose.model.metrics;

import com.emc.mongoose.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.common.collection.OptLockBuffer;
import com.emc.mongoose.model.io.IoType;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.io.Closeable;
import java.io.Serializable;
/**
 Created by andrey on 14.07.16.
 */
public interface IoStats
extends Closeable {
	
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
	
	void refreshLastSnapshot();

	Snapshot getLastSnapshot();
	
	interface Snapshot
	extends Serializable {
		
		String getStepName();
		IoType getIoType();
		int getDriverCount();
		int getConcurrency();
		boolean getVolatileOutputFlag();

		/** @return value in milliseconds */
		long getStartTime();
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
		long getElapsedTime();

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
		/** @return values in microseconds */
		long[] getDurationValues();
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
		/** @return values in microseconds */
		long[] getLatencyValues();
		/** @return value in microseconds */
		double getLatencyMean();
	}
	
	OptLockBuffer<IoStats> OUTPUT_BUFF = new OptLockArrayBuffer<>(1);
	
	static void outputLastStats(
		final Int2ObjectMap<IoStats.Snapshot> lastStats, final Int2IntMap driversCountMap,
		final Int2IntMap concurrencyMap, final String jobName, final boolean volatileOutputFlag
	) {
		Loggers.METRICS_STD_OUT.info(
			new MetricsStdoutLogMessage(jobName, lastStats, concurrencyMap, driversCountMap)
		);
		if(!volatileOutputFlag) {
			Loggers.METRICS_FILE.info(
				new MetricsCsvLogMessage(lastStats, concurrencyMap, driversCountMap)
			);
		}
	}
	
	static void outputLastMedStats(
		final Int2ObjectMap<IoStats.Snapshot> lastStats, final Int2IntMap driversCountMap,
		final Int2IntMap concurrencyMap, final String jobName, final boolean volatileOutputFlag
	) {
		if(!volatileOutputFlag) {
			Loggers.METRICS_THRESHOLD_FILE.info(
				new MetricsCsvLogMessage(lastStats, concurrencyMap, driversCountMap)
			);
		}
	}
}
