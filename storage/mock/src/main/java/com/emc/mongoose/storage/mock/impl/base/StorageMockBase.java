package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.collection.ListingLRUMap;
import com.emc.mongoose.common.concurrent.BlockingQueueTaskSequencer;
import com.emc.mongoose.common.concurrent.FutureTaskBase;
import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.model.impl.item.CsvFileItemInput;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
import com.emc.mongoose.storage.mock.api.StorageIoStats;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.exception.StorageMockCapacityLimitReachedException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 Created on 19.07.16.
 */
public abstract class StorageMockBase<T extends MutableDataItemMock>
implements StorageMock<T> {

	private static final Logger LOG = LogManager.getLogger();

	private final AtomicBoolean started = new AtomicBoolean(false);

	private final String dataSrcPath;
	private final StorageIoStats ioStats;
	protected final ContentSource contentSrc;
	private final int storageCapacity, containerCapacity;

	private final ListingLRUMap<String, ObjectContainerMock<T>> storageMap;
	private final String defaultContainerName;

	private final AtomicBoolean isCapacityExhausted = new AtomicBoolean(false);

	@SuppressWarnings("unchecked")
	public StorageMockBase(
		final Config.StorageConfig.MockConfig mockConfig,
		final Config.LoadConfig.MetricsConfig metricsConfig,
		final Config.ItemConfig itemConfig,
		final ContentSource contentSrc
	) {
		super();
		final Config.StorageConfig.MockConfig.ContainerConfig
			containerConfig = mockConfig.getContainerConfig();
		storageMap = new ListingLRUMap<>(containerConfig.getCountLimit());
		this.dataSrcPath = itemConfig.getInputConfig().getFile();
		this.contentSrc = contentSrc;
		this.ioStats = new BasicStorageIoStats(this, (int) metricsConfig.getPeriod());
		this.storageCapacity = mockConfig.getCapacity();
		this.containerCapacity = containerConfig.getCapacity();
		defaultContainerName = getClass().getSimpleName().toLowerCase();
		createContainer(defaultContainerName);
	}

	protected final ContentSource getContentSource() {
		return contentSrc;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// Container methods
	////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void createContainer(final String name) {
		BlockingQueueTaskSequencer.INSTANCE.submit(new PutContainerTask(name));
	}

	@Override
	public final ObjectContainerMock<T> getContainer(final String name) {
		try {
			final Future<ObjectContainerMock<T>> future =
				BlockingQueueTaskSequencer.INSTANCE.submit(new GetContainerTask(name));
			if(future != null) {
				return future.get();
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container getting was interrupted");
		} catch(final ExecutionException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container get task failure");
		}
		return null;
	}

	@Override
	public final void deleteContainer(final String name) {
		BlockingQueueTaskSequencer.INSTANCE.submit(new DeleteContainerTask(name));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// Object methods
	////////////////////////////////////////////////////////////////////////////////////////////////

	protected abstract T newDataObject(final String id, final long offset, final long size);

	@Override
	public final void createObject(
			final String containerName, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException, StorageMockCapacityLimitReachedException {
		if(isCapacityExhausted.get()) {
			throw new StorageMockCapacityLimitReachedException();
		}
		BlockingQueueTaskSequencer.INSTANCE.submit(new PutObjectTask(containerName, newDataObject(id, offset, size)));
	}

	@Override
	public final void updateObject(
			final String containerName, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final Future<T> future =
				BlockingQueueTaskSequencer.INSTANCE.submit(new GetObjectTask(containerName, id));
			if(future != null) {
				final T obj = future.get();
				if(obj == null) {
					throw new ObjectMockNotFoundException();
				}
				obj.update(offset, size);
			}
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the update task");
		}
	}
	//
	@Override
	public final void appendObject(
			final String containerName, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final Future<T> future =
				BlockingQueueTaskSequencer.INSTANCE.submit(new GetObjectTask(containerName, id));
			if(future != null) {
				final T obj = future.get();
				if(obj == null) {
					throw new ObjectMockNotFoundException();
				}
				obj.append(offset, size);
			}
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the append task");
		}
	}

	@Override
	public final T getObject(
			final String containerName, final String id, final long offset, final long size
	) throws ContainerMockException {
		// TODO partial read using offset and size args
		try {
			final Future<T> future =
				BlockingQueueTaskSequencer.INSTANCE.submit(new GetObjectTask(containerName, id));
			if(future != null) {
				return future.get();
			}
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return null;
	}

	@Override
	public final void deleteObject(
			final String containerName, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException {
		BlockingQueueTaskSequencer.INSTANCE.submit(new DeleteObjectTask(containerName, id));
	}

	@Override
	public final T listObjects(
			final String containerName, final String afterObjectId, final Collection<T> outputBuffer, final int limit
	) throws ContainerMockException {
		try {
			final Future<T> future = BlockingQueueTaskSequencer.INSTANCE.submit(
				new ListObjectsTask(containerName, afterObjectId, outputBuffer, limit));
			if(future != null) {
				return future.get();
			}
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return null;
	}

	private final Thread storageCapacityMonitorThread = new Thread("storageMockCapacityMonitor") {
		{
			setDaemon(true);
		}
		@SuppressWarnings("InfiniteLoopStatement")
		@Override
		public final void run() {
			long currObjCount;
			try {
				while(true) {
					TimeUnit.SECONDS.sleep(1);
					currObjCount = getSize();
					if(!isCapacityExhausted.get() && currObjCount > storageCapacity) {
						isCapacityExhausted.set(true);
					} else if(isCapacityExhausted.get() && currObjCount <= storageCapacity) {
						isCapacityExhausted.set(false);
					}
				}
			} catch(final InterruptedException ignored) {
			}
		}
	};

	@Override
	public final void start() {
		loadPersistedDataItems();
		ioStats.start();
		doStart();
		storageCapacityMonitorThread.start();
		started.set(true);
	}

	@Override
	public final boolean isStarted() {
		return started.get();
	}

	protected abstract void doStart();

	@Override
	public long getSize() {
		final int[] sizes = new int[storageMap.size()];
		ObjectContainerMock<T> container;
		for (int i = 0; i < sizes.length; i++) {
			container = storageMap.getOrDefault(i, null);
			if (container != null) {
				sizes[i] = container.size();
			}
		}
		return IntStream.of(sizes).sum();
	}

	@Override
	public long getCapacity() {
		return storageCapacity;
	}

	@Override
	public final void putIntoDefaultContainer(final List<T> dataItems) {
		BlockingQueueTaskSequencer.INSTANCE.submit(new PutObjectsBatchTask(defaultContainerName, dataItems));
	}

	@Override
	public StorageIoStats getStats() {
		return ioStats;
	}

	@SuppressWarnings({"InfiniteLoopStatement", "unchecked"})
	private void loadPersistedDataItems() {
		if(dataSrcPath != null && !dataSrcPath.isEmpty()) {
			final Path dataFilePath = Paths.get(dataSrcPath);
			if(!Files.exists(dataFilePath)) {
				LOG.warn(
					Markers.ERR, "Data item source file @ \"" + dataSrcPath + "\" doesn't exists"
				);
				return;
			}
			if(Files.isDirectory(dataFilePath)) {
				LOG.warn(
					Markers.ERR, "Data item source file @ \"" + dataSrcPath + "\" is a directory"
				);
				return;
			}
			if(Files.isReadable(dataFilePath)) {
				LOG.debug(
					Markers.ERR, "Data item source file @ \"" + dataSrcPath + "\" is not readable"
				);
			}
			final AtomicLong count = new AtomicLong(0);
			List<T> buff;
			int n;
			final Thread displayProgressThread = new Thread(() -> {
				try {
					while(true) {
						LOG.info(Markers.MSG, "{} items loaded...", count.get());
						TimeUnit.SECONDS.sleep(10);
					}
				} catch(final InterruptedException ignored) {
				}
			});
			try(
				final CsvFileItemInput<T>
					csvFileItemInput = new CsvFileItemInput<>(dataFilePath, (Class<T>) BasicMutableDataItemMock.class, contentSrc)
			) {
				displayProgressThread.start();
				do {
					buff = new ArrayList<>(4096);
					n = csvFileItemInput.get(buff, 4096);
					if(n > 0) {
						putIntoDefaultContainer(buff);
						count.addAndGet(n);
					} else {
						break;
					}
				} while(true);
			} catch(final EOFException e) {
				LOG.info(Markers.MSG, "Loaded {} data items from file {}", count, dataFilePath);
			} catch(final IOException | NoSuchMethodException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to load the data items from file \"{}\"",
					dataFilePath
				);
			} finally {
				displayProgressThread.interrupt();
			}
		}
	}

	private abstract class ContainerTaskBase extends FutureTaskBase<T> {
		private final String containerName;

		ContainerTaskBase(final String containerName) {
			this.containerName = containerName;
		}

		ObjectContainerMock<T> getContainer() {
			return storageMap.get(containerName);
		}

		boolean setException() {
			return setException(new ContainerMockNotFoundException(containerName));
		}
	}

	private class PutObjectsBatchTask extends FutureTaskBase<List<T>> {

		private final String containerName;
		private final List<T> objects;

		PutObjectsBatchTask(final String containerName, final List<T> objects) {
			this.containerName = containerName;
			this.objects = objects;
		}

		@Override
		public void run() {
			final ObjectContainerMock<T> container = storageMap.get(containerName);
			if(container != null) {
				objects.forEach(object -> container.put(object.getName(), object));
				set(new ArrayList<>(container.values()));
			} else {
				setException(new ContainerMockNotFoundException(containerName));
			}
		}

	}

	private class ListObjectsTask
		extends ContainerTaskBase {

		private final String afterObjectId;
		private final Collection<T> outputBuffer;
		private final int limit;

		ListObjectsTask(
			final String containerName, final String afterObjectId,
			final Collection<T> outputBuffer, final int limit
		) {
			super(containerName);
			this.afterObjectId = afterObjectId;
			this.outputBuffer = outputBuffer;
			this.limit = limit;
		}

		@Override
		public void run() {
			final ObjectContainerMock<T> container = getContainer();
			if(container != null) {
				set(container.list(afterObjectId, outputBuffer, limit));
			} else {
				setException();
			}
		}
	}

	private class DeleteObjectTask extends ContainerTaskBase {

		private final String objectId;

		DeleteObjectTask(final String containerName, final String objectId) {
			super(containerName);
			this.objectId = objectId;
		}

		@Override
		public void run() {
			final ObjectContainerMock<T> container = getContainer();
			if(container != null) {
				set(container.remove(objectId));
			} else {
				setException();
			}
		}
	}

	private class GetObjectTask extends ContainerTaskBase {

		private final String objectId;

		GetObjectTask(final String containerName, final String objectId) {
			super(containerName);
			this.objectId = objectId;
		}

		@Override
		public void run() {
			final ObjectContainerMock<T> container = getContainer();
			if(container != null) {
				set(container.get(objectId));
			} else {
				setException();
			}
		}
	}

	private class PutObjectTask extends ContainerTaskBase {

		private T object;

		PutObjectTask(final String containerName, final T object) {
			super(containerName);
			this.object = object;
		}

		@Override
		public void run() {
			final ObjectContainerMock<T> container =
				getContainer();
			if(container != null) {
				set(container.put(object.getName(), object));
			} else {
				setException();
			}
		}
	}

	private class DeleteContainerTask extends FutureTaskBase<ObjectContainerMock<T>> {

		private final String containerName;

		DeleteContainerTask(final String containerName) {
			this.containerName = containerName;
		}

		@Override
		public void run() {
			if(storageMap.containsKey(containerName)) {
				set(storageMap.remove(containerName));
			} else {
				setException(new ContainerMockNotFoundException(containerName));
			}
		}

	}

	private class GetContainerTask extends FutureTaskBase<ObjectContainerMock<T>> {

		private final String containerName;

		GetContainerTask(final String containerName) {
			this.containerName = containerName;
		}

		@Override
		public void run() {
			if(storageMap.containsKey(containerName)) {
				set(storageMap.get(containerName));
			} else {
				setException(new ContainerMockNotFoundException(containerName));
			}
		}

	}

	private class PutContainerTask extends FutureTaskBase<ObjectContainerMock<T>> {

		private final String containerName;

		PutContainerTask(final String containerName) {
			this.containerName = containerName;
		}

		@Override
		public void run() {
			set(storageMap.put(containerName, new BasicObjectContainerMock<>(containerCapacity)));
			ioStats.containerCreate();
		}
	}
}
