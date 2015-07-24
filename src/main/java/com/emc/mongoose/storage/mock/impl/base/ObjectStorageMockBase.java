package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
//
import com.emc.mongoose.core.impl.load.model.AsyncConsumerBase;
//
import com.emc.mongoose.storage.mock.api.ObjectStorage;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 03.07.15.
 */
public abstract class ObjectStorageMockBase<T extends DataObject>
extends StorageMockBase<T>
implements ObjectStorage<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final LRUMap<String, T> itemIndex;
	protected final AsyncConsumer<T> createConsumer, deleteConsumer;
	//
	protected ObjectStorageMockBase(final RunTimeConfig rtConfig, final Class<T> itemCls) {
		super(rtConfig, itemCls);
		itemIndex = new LRUMap<>(rtConfig.getStorageMockCapacity());
		final int
			maxQueueSize = rtConfig.getTasksMaxQueueSize(),
			submTimeOutMilliSec = rtConfig.getTasksSubmitTimeOutMilliSec();
		createConsumer = new AsyncConsumerBase<T>(Long.MAX_VALUE, maxQueueSize, submTimeOutMilliSec) {
			{ setDaemon(true); setName("asyncCreateWorker"); start(); }
			@Override
			protected final void submitSync(final T dataItem)
			throws InterruptedException, RemoteException {
				synchronized(itemIndex) {
					itemIndex.put(dataItem.getId(), dataItem);
				}
			}
		};
		deleteConsumer = new AsyncConsumerBase<T>(Long.MAX_VALUE, maxQueueSize, submTimeOutMilliSec) {
			{ setDaemon(true); setName("asyncDeleteWorker"); start(); }
			@Override
			protected final void submitSync(final T dataItem)
			throws InterruptedException, RemoteException {
				synchronized(itemIndex) {
					itemIndex.remove(dataItem.getId());
				}
			}
		};
	}
	//
	@Override
	protected void startAsyncConsuming() {
		try {
			createConsumer.start();
		} catch(final RemoteException ignored) {
		}
		try {
			deleteConsumer.start();
		} catch(final RemoteException ignored) {
		}
	}
	//
	@Override
	public long getSize() {
		return itemIndex.size();
	}
	//
	@Override
	public long getCapacity() {
		return itemIndex.maxSize();
	}
	//
	@Override
	public void create(final T dataItem) {
		try {
			createConsumer.submit(dataItem);
		} catch(final InterruptedException | RejectedExecutionException | RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Create submission failure");
		}
	}
	//
	@Override
	public void delete(final T dataItem) {
		try {
			deleteConsumer.submit(dataItem);
		} catch(final InterruptedException | RejectedExecutionException | RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Delete submission failure");
		}
	}
	//
	@Override
	public void update(final T dataItem, final long start, final long end) {
		// TODO
	}
	//
	@Override
	public void append(final T dataItem, final long start, final long len) {
		// TODO
	}
	//
	@Override
	public T get(final String id) {
		synchronized(itemIndex) {
			return itemIndex.get(id);
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		itemIndex.clear();
		super.close();
	}
}
