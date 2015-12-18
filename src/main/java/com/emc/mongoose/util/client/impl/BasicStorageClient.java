package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.base.ItemDst;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
//
import com.emc.mongoose.util.client.api.StorageClient;
//
import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 19.06.15.
 */
public class BasicStorageClient<T extends Item>
implements StorageClient<T> {
	//
	protected final static int DEFAULT_CONN_PER_NODE_COUNT = 1;
	//
	protected RunTimeConfig rtConfig;
	protected LoadBuilder<T, LoadExecutor<T>> loadBuilder;
	//
	public BasicStorageClient(
		final RunTimeConfig rtConfig, final LoadBuilder<T, LoadExecutor<T>> loadBuilder
	) {
		this.rtConfig = rtConfig;
		this.loadBuilder = loadBuilder;
	}
	//
	protected long executeLoadJob(
		final LoadExecutor<T> loadExecutor, final ItemDst<T> dst
	) throws InterruptedException, IOException {
		loadExecutor.setItemDst(dst);
		loadExecutor.start();
		final long timeOut = rtConfig.getLoadLimitTimeValue();
		final TimeUnit timeUnit = rtConfig.getLoadLimitTimeUnit();
		try {
			loadExecutor.await(
				timeOut == 0 ? Long.MAX_VALUE : timeOut, timeUnit == null ? TimeUnit.DAYS : timeUnit
			);
		} finally {
			loadExecutor.interrupt();
		}
		return loadExecutor.getLoadState().getStatsSnapshot().getSuccCount();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long write(final long size)
	throws IllegalArgumentException, InterruptedException, IOException {
		return write(null, null, 0, DEFAULT_CONN_PER_NODE_COUNT, size, size, 0);
	}
	//
	@Override
	public long write(
		final ItemSrc<T> src, final ItemDst<T> dst,
		final long maxCount, final int connPerNodeCount, final long size
	) throws IllegalArgumentException, InterruptedException, IOException {
		return write(src, dst, maxCount, connPerNodeCount, size, size, 0);
	}
	//
	@Override
	public long write(
		final ItemSrc<T> src, final ItemDst<T> dst,
		final long maxCount, final int connPerNodeCount,
		final long minSize, final long maxSize, final float sizeBias
	) throws IllegalArgumentException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder)
				.setMinObjSize(minSize)
				.setMaxObjSize(maxSize)
				.setObjSizeBias(sizeBias);
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.CREATE)
				.useNewItemSrc().setItemSrc(src)
				.setMaxCount(maxCount)
				.setConnPerNodeFor(connPerNodeCount, IOTask.Type.CREATE)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long read(final ItemSrc<T> src)
	throws IllegalStateException, InterruptedException, IOException {
		return read(src, null, 0, DEFAULT_CONN_PER_NODE_COUNT, rtConfig.getReadVerifyContent());
	}
	//
	@Override
	public long read(
		final ItemSrc<T> src, final ItemDst<T> dst,
		final long maxCount, final int connPerNodeCount, final boolean verifyContentFlag
	) throws IllegalStateException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder)
				.useContainerListingItemSrc()
				.getIOConfig().setVerifyContentFlag(verifyContentFlag);
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.READ)
				.setItemSrc(src)
				.setMaxCount(maxCount)
				.setConnPerNodeFor(connPerNodeCount, IOTask.Type.READ)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long delete(final ItemSrc<T> src)
	throws IllegalStateException, InterruptedException, IOException {
		return delete(src, null, 0, DEFAULT_CONN_PER_NODE_COUNT);
	}
	//
	@Override
	public long delete(
		final ItemSrc<T> src, final ItemDst<T> dst,
		final long maxCount, final int connPerNodeCount
	) throws IllegalStateException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder).useContainerListingItemSrc();
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.DELETE)
				.setItemSrc(src)
				.setMaxCount(maxCount)
				.setConnPerNodeFor(connPerNodeCount, IOTask.Type.DELETE)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long update(final ItemSrc<T> src)
	throws IllegalStateException, InterruptedException, IOException {
		return update(src, null, 0, DEFAULT_CONN_PER_NODE_COUNT, rtConfig.getUpdateCountPerTime());
	}
	//
	@Override
	public long update(
		final ItemSrc<T> src, final ItemDst<T> dst,
		final long maxCount, final int connPerNodeCount, final int countPerTime
	) throws IllegalArgumentException, IllegalStateException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder).setUpdatesPerItem(countPerTime);
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(IOTask.Type.UPDATE)
				.setItemSrc(src)
				.setMaxCount(maxCount)
				.setConnPerNodeFor(connPerNodeCount, IOTask.Type.UPDATE)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long append(final ItemSrc<T> src, final long size)
	throws IllegalStateException, InterruptedException, IOException {
		return append(src, null, 0, DEFAULT_CONN_PER_NODE_COUNT, size, size, 0);
	}
	//
	@Override
	public long append(
		final ItemSrc<T> src, final ItemDst<T> dst,
		final long maxCount, final int connPerNodeCount, final long size
	) throws IllegalArgumentException, IllegalStateException, InterruptedException, IOException {
		return append(src, dst, maxCount, connPerNodeCount, size, size, 0);
	}
	//
	@Override
	public long append(
		final ItemSrc<T> src, final ItemDst<T> dst,
		final long maxCount, final int connPerNodeCount,
		final long sizeMin, final long sizeMax, final float sizeBias
	) throws IllegalArgumentException, IllegalStateException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder)
				.setMinObjSize(sizeMin)
				.setMaxObjSize(sizeMax)
				.setObjSizeBias(sizeBias);
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setItemSrc(src)
				.setLoadType(IOTask.Type.APPEND)
				.setMaxCount(maxCount)
				.setConnPerNodeFor(connPerNodeCount, IOTask.Type.APPEND)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void close()
	throws IOException {
		loadBuilder.close();
	}
}
