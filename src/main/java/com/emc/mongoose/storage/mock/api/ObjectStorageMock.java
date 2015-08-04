package com.emc.mongoose.storage.mock.api;
//
import java.util.Collection;
import java.util.Map;
/**
 Created by kurila on 03.07.15.
 */
public interface ObjectStorageMock<T extends DataObjectMock>
extends Map<String, ObjectContainerMock<T>>, StorageMock<T> {
	//
	void create(final String container, final String id, final long offset, final long size)
	throws ContainerMockException;
	//
	void delete(final String container, final String id)
	throws ContainerMockException;
	//
	void update(final String container, final String id, final long offset, final long size)
	throws ContainerMockException, ObjectMockNotFoundException;
	//
	void append(final String container, final String id, final long offset, final long size)
	throws ContainerMockException, ObjectMockNotFoundException;
	//
	T read(final String container, final String id, final long offset, final long size)
	throws ContainerMockException, ObjectMockNotFoundException;
	//
	T list(
		final String container, final String marker, final Collection<T> buffDst, final int maxCount
	) throws ContainerMockException;
	//
	void create(final String name);
	//
	ObjectContainerMock<T> get(final String name)
	throws ContainerMockException;
	//
	void delete(final String name);

}
