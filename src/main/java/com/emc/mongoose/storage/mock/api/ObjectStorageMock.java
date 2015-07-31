package com.emc.mongoose.storage.mock.api;
//
import java.util.Collection;
/**
 Created by kurila on 03.07.15.
 */
public interface ObjectStorageMock<T extends DataObjectMock>
extends StorageMock<T> {
	//
	T create(final String container, final String id, final long offset, final long size)
	throws ContainerMockNotFoundException;
	//
	T update(final String container, final String id, final long offset, final long size)
	throws ContainerMockNotFoundException, ObjectMockNotFoundException;
	//
	T append(final String container, final String id, final long offset, final long size)
	throws ContainerMockNotFoundException, ObjectMockNotFoundException;
	//
	T read(final String container, final String id, final long offset, final long size)
	throws ContainerMockNotFoundException, ObjectMockNotFoundException;
	//
	T delete(final String container, final String id)
	throws ContainerMockNotFoundException, ObjectMockNotFoundException;
	//
	ObjectContainerMock<T> create(final String name)
	throws ContainerMockAlreadyExistsException;
	//
	boolean exists(final String name);
	//
	ObjectContainerMock<T> delete(final String name)
	throws ContainerMockNotFoundException;
	//
	String list(
		final String container, final String marker, final Collection<T> buffDst, final int maxCount
	) throws ContainerMockNotFoundException;
}
