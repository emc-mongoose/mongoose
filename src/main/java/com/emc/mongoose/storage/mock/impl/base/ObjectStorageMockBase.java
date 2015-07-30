package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
//
import com.emc.mongoose.core.impl.load.model.AsyncConsumerBase;
//
import com.emc.mongoose.storage.mock.api.ObjectStorageMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
/**
 Created by kurila on 03.07.15.
 */
public abstract class ObjectStorageMockBase<T extends DataObject>
extends StorageMockBase<T>
implements ObjectStorageMock<T> {
	//
	protected final Map<String, T> itemIndex;
	protected final AsyncConsumer<T> createWorker, deleteWorker;
	protected final int capacity;
	//
	protected ObjectStorageMockBase(final RunTimeConfig rtConfig, final Class<T> itemCls) {
		super(rtConfig, itemCls);
		capacity = rtConfig.getStorageMockCapacity();
		itemIndex = new LRUMap<>(rtConfig.getStorageMockCapacity());
		final int maxQueueSize = rtConfig.getTasksMaxQueueSize();
		//
		createWorker = new AsyncConsumerBase<T>(Long.MAX_VALUE, Long.MAX_VALUE, maxQueueSize) {
			{ setName("createWorker"); setDaemon(true); }
			@Override
			protected final void submitSync(final T item)
			throws InterruptedException, RemoteException {
				create(item);
			}
		};
		deleteWorker = new AsyncConsumerBase<T>(Long.MAX_VALUE, Long.MAX_VALUE, maxQueueSize) {
			{ setName("deleteWorker"); setDaemon(true); }
			protected final void submitSync(final T item)
			throws InterruptedException, RemoteException {
				itemIndex.remove(item.getId());
			}
		};
	}
	//
	@Override
	protected void startAsyncConsuming() {
		try {
			createWorker.start();
			deleteWorker.start();
		} catch(final RemoteException e) {
			throw new RuntimeException(e);
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
		return capacity;
	}
	//
	@Override
	public final void create(final T dataItem) {
		itemIndex.put(dataItem.getId(), dataItem);
	}
	//
	@Override
	public void close()
	throws IOException {
		try {
			createWorker.close();
		} finally {
			try {
				deleteWorker.close();
			} finally {
				itemIndex.clear();
				super.close();
			}
		}
	}
}
