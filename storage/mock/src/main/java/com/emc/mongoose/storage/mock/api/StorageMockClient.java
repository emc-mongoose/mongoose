package com.emc.mongoose.storage.mock.api;

import javax.jmdns.ServiceListener;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 Created on 01.09.16.
 */
public interface StorageMockClient<T extends MutableDataItemMock, O extends StorageMockServer<T>>
extends ServiceListener, Closeable {

	void start();

	T readObject(
		final String containerName, final String id, final long offset, final long size
	) throws ExecutionException, InterruptedException;

}
