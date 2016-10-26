package com.emc.mongoose.storage.mock.api;

import java.io.Closeable;

/**
 Created on 19.07.16.
 */
public interface StorageIoStats
extends Runnable, Closeable {

	enum IoType {WRITE, READ, DELETE}

	String METRIC_NAME_CONTAINERS_KEY = "containers";

	void start();

	void markWrite(final boolean success, final long size);
	void markRead(final boolean success, final long size);
	void markDelete(final boolean success);

	void containerCreate();
	void containerDelete();

	double getWriteRate();
	double getWriteRateBytes();

	double getReadRate();
	double getReadRateBytes();

	double getDeleteRate();

}
