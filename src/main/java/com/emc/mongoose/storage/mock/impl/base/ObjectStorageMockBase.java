package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.storage.mock.api.ContainerMockAlreadyExistsException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
import com.emc.mongoose.storage.mock.api.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.ObjectStorageMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 03.07.15.
 */
public abstract class ObjectStorageMockBase<T extends DataObjectMock>
extends StorageMockBase<T>
implements ObjectStorageMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final Map<String, ObjectContainerMock<T>> containersIndex;
	protected final ObjectContainerMock<T> defaultContainer;
	protected final int capacity, containerCapacity;
	protected final Lock lock = new ReentrantLock();
	//
	protected ObjectStorageMockBase(final RunTimeConfig rtConfig, final Class<T> itemCls) {
		super(rtConfig, itemCls);
		capacity = rtConfig.getStorageMockCapacity();
		containerCapacity = rtConfig.getStorageMockContainerCapacity();
		containersIndex = new LRUMap<>(rtConfig.getStorageMockContainerCountLimit());
		defaultContainer = new BasicObjectContainerMock<T>(
			ObjectContainerMock.DEFAULT_NAME, containerCapacity
		);
		containersIndex.put(ObjectContainerMock.DEFAULT_NAME, defaultContainer);
	}
	//
	@Override
	public long getSize() {
		long size = 0;
		for(final ObjectContainerMock<T> c : containersIndex.values()) {
			size += c.size();
		}
		return size;
	}
	//
	@Override
	public long getCapacity() {
		return capacity;
	}
	//
	@Override
	public final void create(final T dataItem) {
		defaultContainer.put(dataItem.getId(), dataItem);
	}
	//
	@Override
	public void close()
	throws IOException {
		lock.lock();
		try {
			for(final ObjectContainerMock<T> container : containersIndex.values()) {
				container.clear();
			}
		} finally {
			lock.unlock();
		}
		containersIndex.clear();
	}
	//
	protected abstract T newDataObject(final String id, final long offset, final long size);
	//
	@Override
	public final T create(
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockNotFoundException {
		final ObjectContainerMock<T> tgtContainer;
		lock.lock();
		try {
			tgtContainer = containersIndex.get(container);
		} finally {
			lock.unlock();
		}
		if(tgtContainer == null) {
			throw new ContainerMockNotFoundException();
		}
		final T obj = newDataObject(oid, offset, size);
		tgtContainer.put(oid, obj);
		return obj;
	}
	//
	@Override
	public final T update(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException, ObjectMockNotFoundException {
		final ObjectContainerMock<T> srcContainer;
		lock.lock();
		try {
			srcContainer = containersIndex.get(container);
		} finally {
			lock.unlock();
		}
		if(srcContainer == null) {
			throw new ContainerMockNotFoundException();
		}
		final T dataObject = srcContainer.get(id);
		dataObject.update(offset, size);
		return dataObject;
	}
	//
	@Override
	public final T append(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException, ObjectMockNotFoundException {
		final ObjectContainerMock<T> srcContainer;
		lock.lock();
		try {
			srcContainer = containersIndex.get(container);
		} finally {
			lock.unlock();
		}
		if(srcContainer == null) {
			throw new ContainerMockNotFoundException();
		}
		final T dataObject = srcContainer.get(id);
		dataObject.append(offset, size);
		return dataObject;
	}
	//
	@Override
	public final T read(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException, ObjectMockNotFoundException {
		// TODO partial read using offset and size args
		final ObjectContainerMock<T> srcContainer;
		lock.lock();
		try {
			srcContainer = containersIndex.get(container);
		} finally {
			lock.unlock();
		}
		if(srcContainer == null) {
			throw new ContainerMockNotFoundException();
		}
		final T dataObject = srcContainer.get(id);
		if(dataObject == null) {
			throw new ObjectMockNotFoundException();
		}
		return dataObject;
	}
	//
	@Override
	public final T delete(final String container, final String id)
	throws ContainerMockNotFoundException, ObjectMockNotFoundException {
		final ObjectContainerMock<T> srcContainer;
		lock.lock();
		try {
			srcContainer = containersIndex.get(container);
		} finally {
			lock.unlock();
		}
		if(srcContainer == null) {
			throw new ContainerMockNotFoundException();
		}
		final T dataObject = srcContainer.get(id);
		if(null == dataObject) {
			throw new ObjectMockNotFoundException();
		}
		return dataObject;
	}
	//
	@Override
	public final ObjectContainerMock<T> create(final String name)
	throws ContainerMockAlreadyExistsException {
		if(containersIndex.containsKey(name)) {
			throw new ContainerMockAlreadyExistsException();
		}
		final ObjectContainerMock<T> c = new BasicObjectContainerMock<>(name, containerCapacity);
		lock.lock();
		try {
			containersIndex.put(name, c);
		} finally {
			lock.unlock();
		}
		return c;
	}
	//
	@Override
	public final boolean exists(final String name) {
		lock.lock();
		try {
			return containersIndex.containsKey(name);
		} finally {
			lock.unlock();
		}
	}
	//
	@Override
	public final ObjectContainerMock<T> delete(final String name)
	throws ContainerMockNotFoundException {
		final ObjectContainerMock<T> c;
		lock.lock();
		try {
			c = containersIndex.remove(name);
		} finally {
			lock.unlock();
		}
		if(c == null) {
			throw new ContainerMockNotFoundException();
		} else {
			c.clear();
		}
		return c;
	}
	//
	@Override
	public final String list(
		final String container, final String marker, final Collection<T> buffDst, final int maxCount
	) throws ContainerMockNotFoundException {
		LOG.info(
			Markers.MSG,
			"Try to get the container \"{}\" listing using marker \"{}\" and count limit {}",
			container, marker, maxCount
		);
		final ObjectContainerMock<T> c;
		lock.lock();
		try {
			c = containersIndex.get(container);
		} finally {
			lock.unlock();
		}
		if(c == null) {
			throw new ContainerMockNotFoundException();
		} else {
			return c.list(marker, buffDst, maxCount);
		}
	}
}
