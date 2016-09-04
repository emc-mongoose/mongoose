package com.emc.mongoose.storage.mock.api;

import javax.jmdns.ServiceListener;
import java.io.Closeable;
import java.util.Collection;

/**
 Created on 01.09.16.
 */
public interface RemoteStorageMockListener<T extends MutableDataItemMock,
	O extends RemoteStorageMock<T>>
extends ServiceListener, Closeable {

	void open();

	Collection<O> getNodes();

	void printNodeList();
}
