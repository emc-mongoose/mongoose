package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.common.concurrent.Daemon;

import javax.jmdns.ServiceListener;
import java.util.concurrent.ExecutionException;

/**
 Created on 01.09.16.
 */
public interface StorageMockClient<T extends MutableDataItemMock>
extends ServiceListener, Daemon {
	T getObject(
		final String containerName, final String id, final long offset, final long size
	) throws ExecutionException, InterruptedException;
}
