package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.conf.SizeInBytes;
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
	protected AppConfig appConfig;
	protected LoadBuilder<T, LoadExecutor<T>> loadBuilder;
	//
	public BasicStorageClient(
		final AppConfig appConfig, final LoadBuilder<T, LoadExecutor<T>> loadBuilder
	) {
		this.appConfig = appConfig;
		this.loadBuilder = loadBuilder;
	}
	//
	protected long executeLoadJob(
		final LoadExecutor<T> loadExecutor, final ItemDst<T> dst
	) throws InterruptedException, IOException {
		loadExecutor.setItemDst(dst);
		loadExecutor.start();
		final long timeOut = appConfig.getLoadLimitTime();
		try {
			loadExecutor.await(timeOut == 0 ? Long.MAX_VALUE : timeOut, TimeUnit.SECONDS);
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
		final ItemSrc<T> src, final ItemDst<T> dst, final long maxCount, final int connPerNodeCount,
		final long size
	) throws IllegalArgumentException, InterruptedException, IOException {
		return write(src, dst, maxCount, connPerNodeCount, size, size, 0);
	}
	//
	@Override
	public long write(
		final ItemSrc<T> src, final ItemDst<T> dst, final long maxCount, final int connPerNodeCount,
		final long minSize, final long maxSize, final float sizeBias
	) throws IllegalArgumentException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder)
				.setDataSize(new SizeInBytes(minSize, maxSize, sizeBias));
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(AppConfig.LoadType.WRITE)
				.useNewItemSrc().setItemSrc(src)
				.setMaxCount(maxCount)
				.setThreadCount(connPerNodeCount)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	//
	@Override
	public long write(
		final ItemSrc<T> src, final ItemDst<T> dst, final long maxCount, final int connPerNodeCount,
		final int randomRangesCount
	) throws IllegalArgumentException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder)
				.setDataRanges(Integer.toString(randomRangesCount));
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(AppConfig.LoadType.WRITE)
				.useNewItemSrc().setItemSrc(src)
				.setMaxCount(maxCount)
				.setThreadCount(connPerNodeCount)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	//
	@Override
	public long write(
		final ItemSrc<T> src, final ItemDst<T> dst, final long maxCount, final int connPerNodeCount,
		final String fixedByteRanges
	) throws IllegalArgumentException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder).setDataRanges(fixedByteRanges);
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(AppConfig.LoadType.WRITE)
				.useNewItemSrc().setItemSrc(src)
				.setMaxCount(maxCount)
				.setThreadCount(connPerNodeCount)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public long read(final ItemSrc<T> src)
	throws IllegalStateException, InterruptedException, IOException {
		return read(src, null, 0, DEFAULT_CONN_PER_NODE_COUNT, appConfig.getItemDataVerify());
	}
	//
	@Override
	public long read(
		final ItemSrc<T> src, final ItemDst<T> dst, final long maxCount, final int connPerNodeCount,
		final boolean verifyContentFlag
	) throws IllegalStateException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder)
				.useContainerListingItemSrc()
				.getIOConfig().setVerifyContentFlag(verifyContentFlag);
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(AppConfig.LoadType.READ)
				.setItemSrc(src)
				.setMaxCount(maxCount)
				.setThreadCount(connPerNodeCount)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	//
	@Override
	public long read(
		final ItemSrc<T> src, final ItemDst<T> dst, final long maxCount, final int connPerNodeCount,
		final boolean verifyContentFlag, final int randomRangesCount
	) throws IllegalStateException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder)
				.setDataRanges(Integer.toString(randomRangesCount))
				.useContainerListingItemSrc()
				.getIOConfig().setVerifyContentFlag(verifyContentFlag);
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(AppConfig.LoadType.READ)
				.setItemSrc(src)
				.setMaxCount(maxCount)
				.setThreadCount(connPerNodeCount)
				.build()
		) {
			return executeLoadJob(loadJobExecutor, dst);
		}
	}
	//
	@Override
	public long read(
		final ItemSrc<T> src, final ItemDst<T> dst, final long maxCount, final int connPerNodeCount,
		final boolean verifyContentFlag, final String fixedByteRanges
	) throws IllegalStateException, InterruptedException, IOException {
		if(loadBuilder instanceof DataLoadBuilder) {
			((DataLoadBuilder) loadBuilder)
				.setDataRanges(fixedByteRanges)
				.useContainerListingItemSrc()
				.getIOConfig().setVerifyContentFlag(verifyContentFlag);
		}
		try(
			final LoadExecutor<T> loadJobExecutor = loadBuilder
				.setLoadType(AppConfig.LoadType.READ)
				.setItemSrc(src)
				.setMaxCount(maxCount)
				.setThreadCount(connPerNodeCount)
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
				.setLoadType(AppConfig.LoadType.DELETE)
				.setItemSrc(src)
				.setMaxCount(maxCount)
				.setThreadCount(connPerNodeCount)
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
