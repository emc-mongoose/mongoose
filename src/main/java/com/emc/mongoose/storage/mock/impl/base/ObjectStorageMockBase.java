package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.DataObject;
//
import com.emc.mongoose.storage.mock.api.ObjectStorageMock;
//
import org.apache.commons.collections4.map.LRUMap;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.07.15.
 */
public abstract class ObjectStorageMockBase<T extends DataObject>
extends StorageMockBase<T>
implements ObjectStorageMock<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final LRUMap<String, T> itemIndex;
	//
	protected final ThreadPoolExecutor taskExecutor;
	//
	protected ObjectStorageMockBase(final RunTimeConfig rtConfig, final Class<T> itemCls) {
		super(rtConfig, itemCls);
		itemIndex = new LRUMap<>(rtConfig.getStorageMockCapacity());
		final int maxQueueSize = rtConfig.getTasksMaxQueueSize();
		taskExecutor = new ThreadPoolExecutor(
			1, 1, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(maxQueueSize),
			new GroupThreadFactory("storageWorker", true),
			new RejectedExecutionHandler() {
				@Override
				public final void rejectedExecution(
					final Runnable task, final ThreadPoolExecutor threadPoolExecutor
				) {
					LOG.warn(Markers.ERR, "Rejected the storage task");
				}
			}
		);
	}
	//
	@Override
	protected void startAsyncConsuming() {
		taskExecutor.prestartCoreThread();
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
	public final void create(final T dataItem) {
		synchronized(itemIndex) {
			itemIndex.put(dataItem.getId(), dataItem);
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		LOG.debug(Markers.MSG, "Dropped {} storage tasks", taskExecutor.shutdownNow().size());
		itemIndex.clear();
		super.close();
	}
}
