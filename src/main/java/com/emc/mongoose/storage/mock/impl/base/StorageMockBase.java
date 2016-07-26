package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.concurrent.Sequencer;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.data.ContentSource;
//
import com.emc.mongoose.core.impl.item.base.CsvFileItemInput;
//
import com.emc.mongoose.storage.mock.api.ContainerMockException;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
import com.emc.mongoose.storage.mock.api.ObjectMockNotFoundException;
import com.emc.mongoose.storage.mock.api.StorageIOStats;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockCapacityLimitReachedException;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.http.concurrent.BasicFuture;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 03.07.15.
 */
public abstract class StorageMockBase<T extends MutableDataItemMock>
extends LRUMap<String, ObjectContainerMock<T>>
implements StorageMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final String dataSrcPath;
	protected final StorageIOStats ioStats;
	protected final Class<T> itemCls;
	protected final ContentSource contentSrc;
	protected final int storageCapacity, containerCapacity;
	//
	private volatile boolean isCapacityExhausted = false;
	//
	protected StorageMockBase(
		final Class<T> itemCls, final ContentSource contentSrc,
		final int storageCapacity, final int containerCapacity, final int containerCountLimit,
		final int batchSize, final String dataSrcPath, final int metricsPeriodSec,
		final boolean jmxServeFlag
	) {
		super(containerCountLimit);
		this.dataSrcPath = dataSrcPath;
		this.itemCls = itemCls;
		this.contentSrc = contentSrc;
		ioStats = new BasicStorageIOStats(this, metricsPeriodSec, jmxServeFlag);
		this.storageCapacity = storageCapacity;
		this.containerCapacity = containerCapacity;
		createContainer(ObjectContainerMock.DEFAULT_NAME);
	}
	//
	@Override
	protected final void moveToMRU(final LinkEntry<String, ObjectContainerMock<T>> entry) {
		// disable entry moving to MRU in case of access
		// it's required to make list method (right below) working (keeping the linked list order)
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Container methods
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void createContainer(final String name) {
		try {
			Sequencer.INSTANCE.submit(new PutContainerTask(name));
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container creation was interrupted");
		}
	}
	//
	private final class PutContainerTask
	extends BasicFuture<ObjectContainerMock<T>>
	implements RunnableFuture<ObjectContainerMock<T>> {
		//
		private final String name;
		//
		private PutContainerTask(final String name) {
			super(null);
			this.name = name;
		}
		//
		@Override
		public final void run() {
			completed(
				StorageMockBase.this.<T>put(
					name, new BasicObjectContainerMock<T>(name, containerCapacity)
				)
			);
			ioStats.containerCreate();
		}
	}
	//
	@Override
	public final ObjectContainerMock<T> getContainer(final String name) {
		ObjectContainerMock<T> c = null;
		try {
			c = Sequencer.INSTANCE.submit(new GetContainerTask(name)).get();
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container getting was interrupted");
		} catch(final ExecutionException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container get task failure");
		}
		return c;
	}
	//
	private final class GetContainerTask
	extends BasicFuture<ObjectContainerMock<T>>
	implements RunnableFuture<ObjectContainerMock<T>> {
		//
		private final String name;
		//
		private GetContainerTask(final String name) {
			super(null);
			this.name = name;
		}
		//
		@Override
		public final void run() {
			completed(StorageMockBase.this.<T>get(name));
		}
	}
	//
	@Override
	public final void deleteContainer(final String name) {
		try {
			Sequencer.INSTANCE.submit(new DeleteContainerTask(name));
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Container deleting was interrupted");
		}
	}
	//
	private final class DeleteContainerTask
	extends BasicFuture<ObjectContainerMock<T>>
	implements RunnableFuture<ObjectContainerMock<T>> {
		//
		private final String name;
		//
		private DeleteContainerTask(final String name) {
			super(null);
			this.name = name;
		}
		//
		@Override
		public final void run() {
			completed(StorageMockBase.this.<T>remove(name));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Object methods
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected abstract T newDataObject(final String id, final long offset, final long size);
	//
	@Override
	public final void createObject(
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockNotFoundException, StorageMockCapacityLimitReachedException {
		if(isCapacityExhausted) {
			throw new StorageMockCapacityLimitReachedException();
		}
		try {
			Sequencer.INSTANCE.submit(new PutObjectTask(container, newDataObject(oid, offset, size)));
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the create task");
		}
	}
	//
	private final class PutObjectTask
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final String container;
		private final T obj;
		//
		private PutObjectTask(final String container, final T obj) {
			super(null);
			this.container = container;
			this.obj = obj;
		}
		//
		@Override
		public final void run() {
			final ObjectContainerMock<T> c = StorageMockBase.this.get(container);
			if(c != null) {
				completed(StorageMockBase.this.get(container).put(obj.getName(), obj));

			} else {
				failed(new ContainerMockNotFoundException(container));
			}
		}
	}
	//
	private final class PutObjectsBatchTask
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final String container;
		private final List<T> objs;
		//
		private PutObjectsBatchTask(final String container, final List<T> objs) {
			super(null);
			this.container = container;
			this.objs = objs;
		}
		//
		@Override
		public final void run() {
			final ObjectContainerMock<T> c = StorageMockBase.this.get(container);
			if(c != null) {
				for(final T obj : objs) {
					StorageMockBase.this.get(container).put(obj.getName(), obj);
				}
				completed(objs.get(0));
			} else {
				failed(new ContainerMockNotFoundException(container));
			}
		}
	}
	//
	private final class GetObjectTask
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final String container, oid;
		//
		private GetObjectTask(final String container, final String oid) {
			super(null);
			this.container = container;
			this.oid = oid;
		}
		//
		@Override
		public final void run() {
			final ObjectContainerMock<T> c = StorageMockBase.this.get(container);
			if(c == null) {
				failed(new ContainerMockNotFoundException(container));
			} else {
				completed(c.get(oid));
			}
		}
	}
	//
	@Override
	public final void updateObject(
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockException, ObjectMockNotFoundException {
		try {
			final T obj = Sequencer.INSTANCE.submit(new GetObjectTask(container, oid)).get();
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
			final T obj = Sequencer.INSTANCE.submit(new GetObjectTask(container, oid)).get();
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
		final String container, final String oid, final long offset, final long size
	) throws ContainerMockException {
		// TODO partial read using offset and size args
		T obj = null;
		try {
			obj = Sequencer.INSTANCE.submit(new GetObjectTask(container, oid)).get();
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return obj;
	}
	//
	private final class DeleteObjectTask
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final String container, oid;
		//
		private DeleteObjectTask(final String container, final String oid) {
			super(null);
			this.container = container;
			this.oid = oid;
		}
		//
		@Override
		public final void run() {
			final ObjectContainerMock<T> c = StorageMockBase.this.get(container);
			if(c == null) {
				failed(new ContainerMockNotFoundException(container));
			} else {
				completed(c.remove(oid));
			}
		}
	}
	//
	@Override
	public final void deleteObject(
		final String container, final String id, final long offset, final long size
	) throws ContainerMockNotFoundException {
		try {
			Sequencer.INSTANCE.submit(new DeleteObjectTask(container, id));
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
			lastObj = Sequencer.INSTANCE
				.submit(new ListObjectTask(container, afterOid, buffDst, limit))
				.get();
		} catch(final ExecutionException e) {
			throw new ContainerMockException(e);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted while submitting the read task");
		}
		return lastObj;
	}
	//
	public final class ListObjectTask
	extends BasicFuture<T>
	implements RunnableFuture<T> {
		//
		private final String container, oid;
		private final Collection<T> buff;
		private final int limit;
		//
		private ListObjectTask(
			final String container, final String oid, final Collection<T> buff, final int limit
		) {
			super(null);
			this.container = container;
			this.oid = oid;
			this.buff = buff;
			this.limit = limit;
		}
		//
		@Override
		public final void run() {
			final ObjectContainerMock<T> c = StorageMockBase.this.get(container);
			if(c == null) {
				failed(new ContainerMockNotFoundException(container));
			} else {
				completed(c.list(oid, buff, limit));
			}
		}
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
	//
	@Override
	public final void start() {
		loadPersistedDataItems();
		ioStats.start();
		startListening();
		storageCapacityMonitorThread.start();
	}
	//
	@Override
	public long getSize() {
		long size = 0;
		for(final ObjectContainerMock<T> c : values()) {
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
		try {
			Sequencer.INSTANCE.submit(new PutObjectsBatchTask(ObjectContainerMock.DEFAULT_NAME, dataItems));
		} catch(final InterruptedException e) {
			LogUtil.exception(
				LOG, Level.WARN, e,
				"Failed to put {} objects into the default container \"{}\"",
				dataItems.size(), ObjectContainerMock.DEFAULT_NAME
			);
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		storageCapacityMonitorThread.interrupt();
		ioStats.close();
		try {
			for(final ObjectContainerMock<T> containerMock : values()) {
				containerMock.clear();
			}
		} catch(final ConcurrentModificationException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to clean up the containers");
		} finally {
			try {
				clear();
			} catch(final ConcurrentModificationException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failed to clean up the storage mock");
			}
		}
	}
	//
	@Override
	public StorageIOStats getStats() {
		return ioStats;
	}
	//
	@Override
	public void run() {
		try {
			start();
		} finally {
			try {
				await();
			} finally {
				try {
					close();
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to close the storage mock");
				}
			}
		}
	}
	//
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
			final Thread displayProgressThread = new Thread(
				new Runnable() {
					@Override
					public final void run() {
						try {
							while(true) {
								LOG.info(Markers.MSG, "{} items loaded...", count.get());
								TimeUnit.SECONDS.sleep(10);
							}
						} catch(final InterruptedException ignored) {
						}
					}
				}
			);
			//
			try(
				final CsvFileItemInput<T>
					csvFileItemInput = new CsvFileItemInput<>(dataFilePath, itemCls, contentSrc)
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
	//
	protected abstract void await();
}
