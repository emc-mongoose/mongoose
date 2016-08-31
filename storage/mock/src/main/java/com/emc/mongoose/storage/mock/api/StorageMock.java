package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.concurrent.Launchable;
import com.emc.mongoose.common.concurrent.RemoteLaunchable;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.StorageMockCapacityLimitReachedException;

import java.io.Closeable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

/**
 Created on 11.07.16. */
public interface StorageMock<T extends MutableDataItemMock>
extends RemoteLaunchable, Closeable, Remote, NodeAggregator<T> {

	long getSize() throws RemoteException;
	long getCapacity() throws RemoteException;
	StorageIoStats getStats() throws RemoteException;
	//
	void putIntoDefaultContainer(final List<T> dataItems) throws RemoteException;
	//
	void createContainer(final String name) throws RemoteException;
	//
	ObjectContainerMock<? extends MutableDataItemMock> getContainer(final String name)
	throws RemoteException;
	//
	void deleteContainer(final String name)
	throws RemoteException;
	//
	T getObject(final String containerName, final String id, final long offset, final long size)
	throws RemoteException;
	//
	void createObject(final String containerName, final String id, final long offset, final long size)
	throws ContainerMockNotFoundException, StorageMockCapacityLimitReachedException,
	RemoteException;
	//
	void deleteObject(final String containerName, final String id, final long offset, final long size)
	throws ContainerMockNotFoundException, RemoteException;
	//
	void updateObject(final String containerName, final String id, final long offset, final long size)
	throws ContainerMockException, ObjectMockNotFoundException, RemoteException;
	//
	void appendObject(final String containerName, final String id, final long offset, final long size)
	throws ContainerMockException, ObjectMockNotFoundException, RemoteException;
	//
	T listObjects(
		final String containerName, final String marker, final Collection<T> outputBuffer, final int maxCount
	) throws ContainerMockException, RemoteException;

}
