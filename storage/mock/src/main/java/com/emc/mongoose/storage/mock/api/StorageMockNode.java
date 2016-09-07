package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.common.concurrent.Daemon;

import java.io.Closeable;

/**
 Created on 07.09.16.
 */
public interface StorageMockNode<T extends MutableDataItemMock, O extends StorageMockServer<T>>
extends Daemon, Closeable {

	StorageMockClient<T, O> client();

	StorageMockServer<T> server();

}
