package com.emc.mongoose.storage.mock.api;
//
import java.io.Closeable;
import java.util.Collection;
/**
 Created by kurila on 03.06.15.
 */
public interface StorageMock<T extends MutableDataItemMock>
extends Runnable, Closeable {
	//
	void start();
	long getSize();
	long getCapacity();
	StorageIOStats getStats();
	//
	void putIntoDefaultContainer(final T dataItem);
	//
	boolean createContainer(final String name)
	throws StorageMockCapacityLimitReachedException;
	//
	ObjectContainerMock<T> getContainer(final String name);
	//
	boolean deleteContainer(final String name);
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
