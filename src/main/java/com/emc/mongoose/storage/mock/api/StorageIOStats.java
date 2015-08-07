package com.emc.mongoose.storage.mock.api;
//
import java.io.Closeable;
/**
 Created by kurila on 18.05.15.
 */
public interface StorageIOStats
extends Runnable, Closeable {
	//
	enum IOType { WRITE, READ, DELETE }
	String METRIC_NAME_CONTAINERS = "containers";
	//
	void start();
	//
	void markWrite(final boolean succ, final long size);
	void markRead(final boolean succ, final long size);
	void markDelete(final boolean succ);
	//
	void containerCreate();
	void containerDelete();
	//
	double getRate();
	double getRateBytes();
}
