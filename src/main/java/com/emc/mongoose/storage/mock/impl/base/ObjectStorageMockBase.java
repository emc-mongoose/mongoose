package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.storage.mock.api.StorageMockCapacityLimitReachedException;
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
import com.emc.mongoose.storage.mock.api.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.ObjectStorageMock;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 03.07.15.
 */
public abstract class ObjectStorageMockBase<T extends DataObjectMock>
extends StorageMockBase<T>
implements ObjectStorageMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final ConcurrentMap<String, ObjectContainerMock<T>> containersIndex;
	protected final ObjectContainerMock<T> defaultContainer;
	protected final int storageCapacity, containerCapacity, containerCountLimit;
	private final AtomicInteger countContainers = new AtomicInteger(0);
	private volatile boolean isCapacityExhausted = false;
	//
	protected ObjectStorageMockBase(final RunTimeConfig rtConfig, final Class<T> itemCls) {
		super(rtConfig, itemCls);
		storageCapacity = rtConfig.getStorageMockCapacity();
		containerCapacity = rtConfig.getStorageMockContainerCapacity();
		containerCountLimit = rtConfig.getStorageMockContainerCountLimit();
		final int expectConcurrencyLevel = rtConfig.getStorageMockHeadCount() *
			rtConfig.getStorageMockIoThreadsPerSocket();
		containersIndex = new ConcurrentHashMap<>(
			containerCountLimit, 0.75f, expectConcurrencyLevel
		);
		defaultContainer = new BasicObjectContainerMock<>(
			ObjectContainerMock.DEFAULT_NAME, containerCapacity
		);
		containersIndex.put(ObjectContainerMock.DEFAULT_NAME, defaultContainer);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Container methods
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean createContainer(final String name)
	throws StorageMockCapacityLimitReachedException {
		if(countContainers.incrementAndGet() > containerCountLimit) {
			countContainers.decrementAndGet();
			throw new StorageMockCapacityLimitReachedException();
		}
		return null == containersIndex
			.putIfAbsent(name, new BasicObjectContainerMock<T>(name, containerCapacity));
	}
	//
	@Override
	public final ObjectContainerMock<T> getContainer(final String name) {
		return containersIndex.get(name);
	}
	//
	@Override
	public final boolean deleteContainer(final String name) {
		final ObjectContainerMock<T> c = containersIndex.remove(name);
		if(c != null) {
			countContainers.decrementAndGet();
			return true;
		} else {
			return false;
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Object methods
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void createObject(
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockNotFoundException, StorageMockCapacityLimitReachedException {
		if(isCapacityExhausted) {
			throw new StorageMockCapacityLimitReachedException();
		}
		try {
			final ObjectContainerMock<T> c = getContainer(container);
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			final T obj = newDataObject(oid, offset, size);
			c.submitPut(oid, obj);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the create task");
		}
	}
	//
	@Override
	public final void updateObject(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final ObjectContainerMock<T> c = getContainer(container);
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			final T obj = c.submitGet(id).get();
			if(obj == null) {
				throw new ObjectMockNotFoundException();
			}
			obj.update(offset, size);
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the update task");
		}
	}
	//
	@Override
	public final void appendObject(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final ObjectContainerMock<T> c = getContainer(container);
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			final T obj = c.submitGet(id).get();
			if(obj == null) {
				throw new ObjectMockNotFoundException();
			}
			obj.append(offset, size);
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the append task");
		}
	}
	//
	@Override
	public final T getObject(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException {
		// TODO partial read using offset and size args
		T obj = null;
		try {
			final ObjectContainerMock<T> c = getContainer(container);
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			obj = c.submitGet(id).get();
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return obj;
	}
	//
	@Override
	public final void deleteObject(final String container, final String id)
	throws ContainerMockNotFoundException {
		try {
			final ObjectContainerMock<T> c = getContainer(container);
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			c.submitRemove(id);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
	}
	//
	@Override
	public final T listObjects(
		final String container, final String afterOid, final Collection<T> buffDst, final int limit
	) throws ContainerMockException {
		T lastObj = null;
		try {
			final ObjectContainerMock<T> c = getContainer(container);
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			lastObj = c.submitList(afterOid, buffDst, limit).get();
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return lastObj;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Misc methods
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final Thread storageCapacityMonitorThread = new Thread("storageMockCapacityMonitor") {
		{
			setDaemon(true);
		}
		@Override
		public final void run() {
			long currObjCount;
			try {
				while(true) {
					Thread.sleep(500);
					currObjCount = getSize();
					if(!isCapacityExhausted && currObjCount > storageCapacity) {
						isCapacityExhausted = true;
					} else if(isCapacityExhausted && currObjCount <= storageCapacity) {
						isCapacityExhausted = false;
					}
				}
			} catch(final InterruptedException ignored) {
			}
		}
	};
	//
	@Override
	public final void start() {
		super.start();
		storageCapacityMonitorThread.start();
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
		return storageCapacity;
	}
	//
	@Override
	public final void putIntoDefaultContainer(final T dataItem) {
		try {
			defaultContainer.submitPut(dataItem.getId(), dataItem).get();
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
		for(final ObjectContainerMock<T> container : containersIndex.values()) {
			container.close();
		}
		containersIndex.clear();
		storageCapacityMonitorThread.interrupt();
	}
	//
	protected abstract T newDataObject(final String id, final long offset, final long size);
}
