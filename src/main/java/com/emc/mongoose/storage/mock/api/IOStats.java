package com.emc.mongoose.storage.mock.api;
//
import java.io.Closeable;
/**
 Created by kurila on 18.05.15.
 */
public interface IOStats
extends Runnable, Closeable {
	//
	String METRIC_COUNT = "count", ALL_METHODS = "all";
	//
	void start();
	//
	void markCreate(final long size);
	void markRead(final long size);
	void markDelete();
	//
	double getMeanRate();
	double getWriteRate();
	double getWriteRateBytes();
	double getReadRate();
	double getReadRateBytes();
}
