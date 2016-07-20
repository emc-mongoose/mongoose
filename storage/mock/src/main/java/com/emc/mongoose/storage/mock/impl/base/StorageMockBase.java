package com.emc.mongoose.storage.mock.impl.base;

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
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.commons.collections4.map.LRUMap;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 Created on 19.07.16.
 */
public abstract class StorageMockBase<T extends MutableDataItemMock> implements StorageMock<T> {

	private static final Logger LOG = LogManager.getLogger();
	private static final int DEFAULT_BATCH_SIZE = 0x1000;

	protected final String dataSrcPath;
	protected final StorageIoStats ioStats;
	protected final Class<T> itemClass;
	protected final ContentSource contentSrc;
	protected final int storageCapacity, containerCapacity;

	private final Map<String, ObjectContainerMock<T>> storageMap;
	private final Sequencer sequencer;

	private AtomicBoolean isCapacityExhausted = new AtomicBoolean(false);

	@SuppressWarnings("unchecked")
	public StorageMockBase(
		final Config.StorageConfig.MockConfig mockConfig,
		final Config.LoadConfig.MetricsConfig metricsConfig,
		final Config.ItemConfig itemConfig) {
		final Config.StorageConfig.MockConfig.ContainerConfig containerConfig =
			mockConfig.getContainerConfig();
		storageMap = new LRUMap<>(containerConfig.getCountLimit());
		this.dataSrcPath = itemConfig.getInputConfig().getFile();
		this.itemClass = (Class<T>) BasicMutableDataItemMock.class;
		try {
			this.contentSrc = ContentSourceUtil.getInstance(itemConfig.getDataConfig().getContentConfig());
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to set a content source"
			);
			throw new IllegalStateException();
		}
		this.ioStats = new BasicStorageIoStats(this, (int) metricsConfig.getPeriod());
		this.storageCapacity = mockConfig.getCapacity();
		this.containerCapacity = containerConfig.getCapacity();
		this.sequencer = new Sequencer("storageMockSequencer", true, DEFAULT_BATCH_SIZE);
		createContainer(getClass().getSimpleName().toLowerCase());
	}

	@Override
	public void createContainer(final String name) {
		try {
			sequencer.submit(new PutContainerTask(name));
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container creation was interrupted");
		}
	}

	@Override
	public final ObjectContainerMock<T> getContainer(final String name) {
		ObjectContainerMock<T> c = null;
		try {
			c = sequencer.submit(new GetContainerTask(name)).get();
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container getting was interrupted");
		} catch(final ExecutionException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container get task failure");
		}
		return c;
	}

	@Override
	public final void deleteContainer(final String name) {
		try {
			sequencer.submit(new DeleteContainerTask(name));
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container deleting was interrupted");
		}
	}

	protected abstract T newDataObject(final String id, final long offset, final long size);

	@Override
	public final void createObject(
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockNotFoundException, StorageMockCapacityLimitReachedException {
		if(isCapacityExhausted.get()) {
			throw new StorageMockCapacityLimitReachedException();
		}
		try {
			sequencer.submit(new PutObjectTask(container, newDataObject(oid, offset, size)));
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the create task");
		}
	}

	@Override
	public final void updateObject(
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final T obj = sequencer.submit(new GetObjectTask(container, oid)).get();
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
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final T obj = sequencer.submit(new GetObjectTask(container, oid)).get();
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

	@Override
	public final T getObject(
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockException {
		// TODO partial read using offset and size args
		T obj = null;
		try {
			obj = sequencer.submit(new GetObjectTask(container, oid)).get();
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return obj;
	}

	@Override
	public final void deleteObject(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException {
		try {
			sequencer.submit(new DeleteObjectTask(container, id));
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
			lastObj = sequencer
				.submit(new ListObjectTask(container, afterOid, buffDst, limit))
				.get();
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return lastObj;
	}

	private final Thread storageCapacityMonitorThread = new Thread("storageMockCapacityMonitor") {
		{
			setDaemon(true);
		}
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
		startListening();
		storageCapacityMonitorThread.start();
		sequencer.start();
	}
	//
	@Override
	public long getSize() {
		long size = 0;
		for(final ObjectContainerMock<T> c : storageMap.values()) { //TODO check are values right?
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
	public final void putIntoDefaultContainer(final List<T> dataItems) {
		final String defaultContainerName = getClass().getSimpleName().toLowerCase();
		try {

			sequencer.submit(new PutObjectsBatchTask(defaultContainerName, dataItems));
		} catch(final InterruptedException e) {
			LogUtil.exception(
				LOG, Level.WARN, e,
				"Failed to put {} objects into the default container \"{}\"",
				dataItems.size(), defaultContainerName
			);
		}
	}

	@Override
	public StorageIoStats getStats() {
		return ioStats;
	}

	protected void loadPersistedDataItems() {
		// if there is data src file path
		if(dataSrcPath != null && !dataSrcPath.isEmpty()) {
			final Path dataFilePath = Paths.get(dataSrcPath);
			//final int dataSizeRadix = appConfig.getDataRadixSize();
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
				//return;
			}
			//
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
			//
			try(
				final CsvFileItemInput<T>
					csvFileItemInput = new CsvFileItemInput<>(dataFilePath, itemClass, contentSrc)
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
	//
	protected abstract void startListening();

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
			if (container != null) {
				objects.forEach(object -> container.put(object.getName(), object));
				set(new ArrayList<>(container.values()));
			} else {
				setException(new ContainerMockNotFoundException(containerName));
			}
		}

	}

	private class ListObjectTask extends ContainerTaskBase {

		private final String afterObjectId;
		private final Collection<T> outputBuffer;
		private final int limit;

		ListObjectTask(
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
			if (storageMap.containsKey(containerName)) {
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
			if (storageMap.containsKey(containerName)) {
				set(storageMap.get(containerName));
			} else {
				setException(new ContainerMockNotFoundException(containerName));
			}
		}

	}

	private class PutContainerTask extends FutureTaskBase<ObjectContainerMock<T>> {

		private final String containerName;

		public PutContainerTask(final String containerName) {
			this.containerName = containerName;
		}

		@Override
		public void run() {
			set(storageMap.put(containerName, new BasicObjectContainerMock<>(containerCapacity)));
			ioStats.containerCreate();
		}
	}
}
