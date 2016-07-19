package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.model.impl.item.CsvFileItemInput;
import com.emc.mongoose.model.impl.item.CsvItemInput;
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
		for(final ObjectContainerMock<T> c : storageMap.values()) { //TODO chech are values right?
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

	private class PutObjectsBatchTask
		implements RunnableFuture<T> {

		private String containerName;
		private List<T> dataItems;

		public PutObjectsBatchTask(final String containerName, final List<T> dataItems) {
			this.containerName = containerName;
			this.dataItems = dataItems;
		}

		@Override
		public void run() {
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public T get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public T get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}

	private class ListObjectTask
		implements RunnableFuture<T> {

		public ListObjectTask(
			final String container, final String afterOid, final Collection<T> buffDst,
			final int limit
		) {

		}

		@Override
		public void run() {
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public T get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public T get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}

	private class DeleteObjectTask
		implements RunnableFuture<T> {

		public DeleteObjectTask(final String container, final String id) {

		}

		@Override
		public void run() {
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public T get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public T get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}

	private class GetObjectTask
		implements RunnableFuture<T> {

		public GetObjectTask(final String container, final String oid) {

		}

		@Override
		public void run() {
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public T get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public T get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}

	private class PutObjectTask
		implements RunnableFuture<T> {

		private String containerName;
		private T dataItem;

		public PutObjectTask(final String containerName, final T dataItems) {
			this.containerName = containerName;
			this.dataItem = dataItem;
		}

		@Override
		public void run() {
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public T get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public T get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}

	private class DeleteContainerTask
		implements RunnableFuture<T> {

		public DeleteContainerTask(
			final String name
		) {
		}

		@Override
		public void run() {
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public T get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public T get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}

	private class GetContainerTask
		implements RunnableFuture<ObjectContainerMock<T>> {

		public GetContainerTask(
			final String name
		) {
		}

		@Override
		public void run() {
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public ObjectContainerMock<T> get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public ObjectContainerMock<T> get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}

	private class PutContainerTask
		implements RunnableFuture<T> {

		public PutContainerTask(
			final String name
		) {

		}

		@Override
		public void run() {
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public T get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public T get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}
}
