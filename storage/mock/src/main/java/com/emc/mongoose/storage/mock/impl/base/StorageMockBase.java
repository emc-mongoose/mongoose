package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.collection.ListingLRUMap;
import com.emc.mongoose.model.api.data.ContentSource;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
	private final ObjectContainerMock<T> defaultContainer;

	private volatile boolean isCapacityExhausted = false;

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
		this.defaultContainer = new BasicObjectContainerMock<>(containerCapacity);
		storageMap.put(getClass().getSimpleName().toLowerCase(), defaultContainer);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// Container methods
	////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public final void createContainer(final String name) {
		synchronized(storageMap) {
			storageMap.put(name, new BasicObjectContainerMock<>(containerCapacity));
		}
	}

	@Override
	public final ObjectContainerMock<T> getContainer(final String name) {
		synchronized(storageMap) {
			return storageMap.get(name);
		}
	}

	@Override
	public final void deleteContainer(final String name) {
		synchronized(storageMap) {
			storageMap.remove(name);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// Object methods
	////////////////////////////////////////////////////////////////////////////////////////////////

	protected abstract T newDataObject(final String id, final long offset, final long size);

	@Override
	public final void createObject(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException, StorageMockCapacityLimitReachedException {
		if(isCapacityExhausted) {
			throw new StorageMockCapacityLimitReachedException();
		}
		final ObjectContainerMock<T> c = getContainer(containerName);
		if(c != null) {
			c.put(id, newDataObject(id, offset, size));
		} else {
			throw new ContainerMockNotFoundException(containerName);
		}
	}

	@Override
	public final void updateObject(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		final ObjectContainerMock<T> c = getContainer(containerName);
		if(c != null) {
			final T obj = c.get(id);
			if(obj != null) {
				obj.update(offset, size);
			} else {
				throw new ObjectMockNotFoundException(id);
			}
		} else {
			throw new ContainerMockNotFoundException(containerName);
		}
	}
	//
	@Override
	public final void appendObject(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		final ObjectContainerMock<T> c = getContainer(containerName);
		if(c != null) {
			final T obj = c.get(id);
			if(obj != null) {
				obj.append(offset, size);
			} else {
				throw new ObjectMockNotFoundException(id);
			}
		} else {
			throw new ContainerMockNotFoundException(containerName);
		}
	}

	@Override
	public final T getObject(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockException {
		// TODO partial read using offset and size args
		final ObjectContainerMock<T> c = getContainer(containerName);
		if(c != null) {
			return c.get(id);
		} else {
			throw new ContainerMockNotFoundException(containerName);
		}
	}

	@Override
	public final void deleteObject(
		final String containerName, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException {
		final ObjectContainerMock<T> c = getContainer(containerName);
		if(c != null) {
			c.remove(id);
		} else {
			throw new ContainerMockNotFoundException(containerName);
		}
	}

	@Override
	public final T listObjects(
		final String containerName, final String afterObjectId, final Collection<T> outputBuffer,
		final int limit
	) throws ContainerMockException {
		final ObjectContainerMock<T> container = getContainer(containerName);
		if(container != null) {
			return container.list(afterObjectId, outputBuffer, limit);
		} else {
			throw new ContainerMockNotFoundException(containerName);
		}
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
		long size = 0;
		synchronized(storageMap) {
			for(final ObjectContainerMock<T> container : storageMap.values()) {
				size += container.size();
			}
		}
		return size;
	}

	@Override
	public long getCapacity() {
		return storageCapacity;
	}

	@Override
	public final void putIntoDefaultContainer(final List<T> dataItems) {
		for(final T object : dataItems) {
			defaultContainer.put(object.getName(), object);
		}
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
					csvFileItemInput = new CsvFileItemInput<>(
					dataFilePath, (Class<T>) BasicMutableDataItemMock.class, contentSrc
				)
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
}
