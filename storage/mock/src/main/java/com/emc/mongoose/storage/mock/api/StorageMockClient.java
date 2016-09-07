package com.emc.mongoose.storage.mock.api;

import javax.jmdns.ServiceListener;
import java.io.Closeable;
import java.util.Collection;

/**
 Created on 01.09.16.
 */
public interface StorageMockClient<T extends MutableDataItemMock,
	O extends StorageMockServer<T>>
extends ServiceListener, Closeable {

	void start();

	Collection<O> getNodes();

	void printNodeList();

}
