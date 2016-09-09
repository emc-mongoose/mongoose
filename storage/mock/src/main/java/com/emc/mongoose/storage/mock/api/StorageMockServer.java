package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;

import java.io.Closeable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 Created on 31.08.16.
 */
public interface StorageMockServer<T extends MutableDataItemMock>
extends Remote, Closeable {

	void start()
	throws UserShootHisFootException, RemoteException;

	boolean isStarted()
	throws RemoteException;

	boolean await()
	throws InterruptedException, RemoteException;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException;

	T getObjectRemotely(
		final String containerName, final String id, final long offset, final long size
	) throws RemoteException, ContainerMockException;

}
