package com.emc.mongoose.storage.mock.api;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.common.concurrent.Launchable;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.StorageMockCapacityLimitReachedException;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;

/**
 Created on 11.07.16. */
public interface StorageMock<T extends MutableDataItemMock>
extends Launchable, Closeable {

	void start();
	long getSize();
	long getCapacity();
	StorageIoStats getStats();
	//
	void putIntoDefaultContainer(final List<T> dataItems);
	//
	void createContainer(final String name);
	//
	ObjectContainerMock<? extends MutableDataItemMock> getContainer(final String name);
	//
	void deleteContainer(final String name);
	//
	void createObject(final String container, final String id, final long offset, final long size)
	throws ContainerMockNotFoundException, StorageMockCapacityLimitReachedException;
	//
	T getObject(final String container, final String id, final long offset, final long size)
	throws ContainerMockException;
	//
	void deleteObject(final String container, final String id, final long offset, final long size)
	throws ContainerMockNotFoundException;
	//
	void updateObject(final String container, final String id, final long offset, final long size)
	throws ContainerMockException, ObjectMockNotFoundException;
	//
	void appendObject(final String container, final String id, final long offset, final long size)
	throws ContainerMockException, ObjectMockNotFoundException;
	//
	T listObjects(
		final String container, final String marker, final Collection<T> buffDst, final int maxCount
	) throws ContainerMockException;

}
