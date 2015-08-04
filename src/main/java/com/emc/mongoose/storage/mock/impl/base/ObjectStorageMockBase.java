package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.concurrent.Sequencer;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
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
	private final Sequencer seqWorker;
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
		seqWorker = new Sequencer(
			"containersSynchronizer", true, rtConfig.getTasksMaxQueueSize(),
			rtConfig.getBatchSize(), 10
		);
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
		if(seqWorker.isAlive()) {
			seqWorker.interrupt();
		}
		for(final ObjectContainerMock<T> container : containersIndex.values()) {
			container.close();
		}
		containersIndex.clear();
	}
	//
	protected abstract T newDataObject(final String id, final long offset, final long size);
	////////////////////////////////////////////////////////////////////////////////////////////////
	public static final class PutContainerTask<T extends DataObjectMock>
	extends FutureTask<ObjectContainerMock<T>> {
		//
		private final static class PutContainerCall<T extends DataObjectMock>
		implements Callable<ObjectContainerMock<T>> {
			//
			private final ObjectStorageMock<T> index;
			private final String name;
			private final ObjectContainerMock<T> container;
			//
			private PutContainerCall(
				final ObjectStorageMock<T> index, final String name,
				final ObjectContainerMock<T> container
			) {
				this.index = index;
				this.name = name;
				this.container = container;
			}
			//
			@Override
			public final ObjectContainerMock<T> call()
			throws Exception {
				return index.put(name, container);
			}
		}
		//
		public PutContainerTask(
			final ObjectStorageMock<T> index, final String name,
			final ObjectContainerMock<T> container
		) {
			super(new PutContainerCall<>(index, name, container));
		}
	}
	//
	public static final class GetContainerTask<T extends DataObjectMock>
	extends FutureTask<ObjectContainerMock<T>> {
		//
		private final static class GetContainerCall<T extends DataObjectMock>
		implements Callable<ObjectContainerMock<T>> {
			//
			private final ObjectStorageMock<T> index;
			private final String name;
			//
			private GetContainerCall(final ObjectStorageMock<T> index, final String name) {
				this.index = index;
				this.name = name;
			}
			//
			@Override
			public final ObjectContainerMock<T> call()
			throws Exception {
				return index.get(name);
			}
		}
		//
		public GetContainerTask(final ObjectStorageMock<T> index, final String name) {
			super(new GetContainerCall<>(index, name));
		}
	}
	//
	public static final class RemoveContainerTask<T extends DataObjectMock>
	extends FutureTask<ObjectContainerMock<T>> {
		//
		private final static class RemoveContainerCall<T extends DataObjectMock>
		implements Callable<ObjectContainerMock<T>> {
			//
			private final ObjectStorageMock<T> index;
			private final String name;
			//
			private RemoveContainerCall(final ObjectStorageMock<T> index, final String name) {
				this.index = index;
				this.name = name;
			}
			//
			@Override
			public final ObjectContainerMock<T> call()
			throws Exception {
				return index.remove(name);
			}
		}
		//
		public RemoveContainerTask(final ObjectStorageMock<T> index, final String name) {
			super(new RemoveContainerCall<>(index, name));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void create(
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockException {
		try {
			final ObjectContainerMock<T> c = seqWorker
				.submit(new GetContainerTask<>(this, container))
				.get();
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			final T obj = newDataObject(oid, offset, size);
			c.submitPut(oid, obj);
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the create task");
		}
	}
	//
	@Override
	public final void update(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final ObjectContainerMock<T> c = seqWorker
				.submit(new GetContainerTask<>(this, container))
				.get();
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
	public final void append(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final ObjectContainerMock<T> c = seqWorker
				.submit(new GetContainerTask<>(this, container))
				.get();
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
	public final T read(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		// TODO partial read using offset and size args
		T obj = null;
		try {
			final ObjectContainerMock<T> c = seqWorker
				.submit(new GetContainerTask<>(this, container))
				.get();
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			obj = c.submitGet(id).get();
			if(obj == null) {
				throw new ObjectMockNotFoundException();
			}
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return obj;
	}
	//
	@Override
	public final void delete(final String container, final String id)
	throws ContainerMockException {
		try {
			final ObjectContainerMock<T> c = seqWorker
				.submit(new GetContainerTask<>(this, container))
				.get();
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
			c.submitRemove(id);
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
	}
	//
	@Override
	public final T list(
		final String container, final String afterOid, final Collection<T> buffDst, final int limit
	) throws ContainerMockException {
		T lastObj = null;
		try {
			final ObjectContainerMock<T> c = seqWorker
				.submit(new GetContainerTask<>(this, container))
				.get();
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
	//
	@Override
	public final void create(final String name) {
		try {
			seqWorker.submit(
				new PutContainerTask<>(
					this, name, new BasicObjectContainerMock<T>(name, containerCapacity)
				)
			);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
	}
	//
	@Override
	public final ObjectContainerMock<T> get(final String name)
	throws ContainerMockException {
		ObjectContainerMock<T> c = null;
		try {
			c = seqWorker
				.submit(new GetContainerTask<>(this, name))
				.get();
			if(c == null) {
				throw new ContainerMockNotFoundException();
			}
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return c;
	}
	//
	@Override
	public final void delete(final String name) {
		try {
			seqWorker.submit(new RemoveContainerTask<>(this, name));
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
	}
}
