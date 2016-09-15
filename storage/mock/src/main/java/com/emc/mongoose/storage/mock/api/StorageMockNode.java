package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.common.concurrent.Daemon;

import java.io.Closeable;

/**
 Created on 07.09.16.
 */
public interface StorageMockNode<T extends MutableDataItemMock>
extends Daemon {

	StorageMockClient<T> client();

	StorageMockServer<T> server();

}
