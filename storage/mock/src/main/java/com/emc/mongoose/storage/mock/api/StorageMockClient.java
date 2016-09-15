package com.emc.mongoose.storage.mock.api;

import javax.jmdns.ServiceListener;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 Created on 01.09.16.
 */
public interface StorageMockClient<T extends MutableDataItemMock>
extends ServiceListener, Closeable {

	void start();

	T getObject(
		final String containerName, final String id, final long offset, final long size
	) throws ExecutionException, InterruptedException;

}
