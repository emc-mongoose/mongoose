package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.ItemBlockingQueue;
import com.emc.mongoose.storage.mock.api.ContainerMockAlreadyExistsException;
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
import com.emc.mongoose.storage.mock.api.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.ObjectStorageMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
		try {
			defaultContainer.put(dataItem.getId(), dataItem).get();
		} catch(final InterruptedException | ExecutionException e) {
			LogUtil.exception(
				LOG, Level.WARN, e,
				"Failed to put the object \"{}\" into the default container \"{}\"",
				dataItem.getId(), defaultContainer.getName()
			);
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		lock.lock();
		try {
			for(final ObjectContainerMock<T> container : containersIndex.values()) {
				container.close();
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
	public final void create(
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
		try {
			tgtContainer.put(oid, obj);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the create task");
		}
	}
	//
	@Override
	public final void update(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
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
		try {
			final T dataObject = srcContainer.get(id).get();
			dataObject.update(offset, size);
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the update task");
		}
	}
	//
	@Override
	public final void append(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
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
		try {
			final T dataObject = srcContainer.get(id).get();
			dataObject.update(offset, size);
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the update task");
		}
	}
	//
	@Override
	public final T read(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
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
		try {
			final T dataObject = srcContainer.get(id).get();
			if(dataObject == null) {
				throw new ObjectMockNotFoundException();
			}
			return dataObject;
		} catch(final ExecutionException | InterruptedException e) {
			throw new ContainerMockException(e);
		}
	}
	//
	@Override
	public final void delete(final String container, final String id)
	throws ContainerMockNotFoundException {
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
		try {
			srcContainer.remove(id);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the delete task");
		}
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
			try {
				c.close();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to delete the container \"{}\"", name
				);
			}
		}
		return c;
	}
	//
	@Override
	public final T list(
		final String container, final String afterOid, final Collection<T> buffDst, final int limit
	) throws ContainerMockException {
		LOG.info(
			Markers.MSG,
			"Try to get the container \"{}\" listing using marker \"{}\" and count limit {}",
			container, afterOid, limit
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
			try {
				return c.list(afterOid, buffDst, limit).get();
			} catch(final ExecutionException | InterruptedException e) {
				throw new ContainerMockException(e);
			}
		}
	}
}
