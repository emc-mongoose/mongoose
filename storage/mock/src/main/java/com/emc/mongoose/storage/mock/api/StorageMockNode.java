package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.common.exception.UserShootHisFootException;

import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 Created on 07.09.16.
 */
public interface StorageMockNode<T extends MutableDataItemMock, O extends StorageMockServer<T>>
extends Closeable {

	StorageMockClient<T, O> client();

	StorageMockServer<T> server();

	void start()
	throws UserShootHisFootException, RemoteException;

	boolean isStarted() throws RemoteException;

	boolean await()
	throws InterruptedException, RemoteException;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException;

}
