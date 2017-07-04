package com.emc.mongoose.tests.perf.util.mock;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.storage.Credential;
import com.emc.mongoose.storage.driver.net.base.NetStorageDriverBase;
import com.emc.mongoose.storage.driver.net.base.pool.NonBlockingConnPool;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collections;
import java.util.List;

/**
 Created by andrey on 12.05.17.
 */
public final class NetStorageDriverMock<I extends Item, O extends IoTask<I>>
extends NetStorageDriverBase<I, O> {

	public NetStorageDriverMock(
		final String jobName, final ContentSource contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException {
		super(jobName, contentSrc, loadConfig, storageConfig, verifyFlag);
	}

	@Override
	protected final NonBlockingConnPool createConnectionPool() {
		return new BasicMultiNodeConnPoolMock(
			concurrencyLevel, concurrencyThrottle, storageNodeAddrs, bootstrap, this,
			storageNodePort
		);
	}

	@Override
	protected final Channel getUnpooledConnection()
	throws ConnectException, InterruptedException {
		return new EmbeddedChannel();
	}

	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {
		return Collections.emptyList();
	}

	@Override
	protected final String requestNewPath(final String path) {
		return path;
	}

	@Override
	protected final String requestNewAuthToken(final Credential credential) {
		return credential == null ? "" : credential.toString();
	}

	@Override
	protected final void sendRequest(
		final Channel channel, final ChannelPromise channelPromise, final O ioTask
	) {
		ioTask.finishRequest();
		ioTask.startResponse();
		if(ioTask instanceof DataIoTask) {
			final DataIoTask dataIoTask = (DataIoTask) ioTask;
			final DataItem dataItem = dataIoTask.getItem();
			switch(dataIoTask.getIoType()) {
				case CREATE:
					try {
						dataIoTask.setCountBytesDone(dataItem.size());
					} catch(final IOException ignored) {
					}
					break;
				case READ:
					dataIoTask.startDataResponse();
				case UPDATE:
					final List<ByteRange> fixedByteRanges = dataIoTask.getFixedRanges();
					if(fixedByteRanges == null || fixedByteRanges.isEmpty()) {
						if(dataIoTask.hasMarkedRanges()) {
							dataIoTask.setCountBytesDone(dataIoTask.getMarkedRangesSize());
						} else {
							try {
								dataIoTask.setCountBytesDone(dataItem.size());
							} catch(final IOException e) {
							}
						}
					} else {
						dataIoTask.setCountBytesDone(dataIoTask.getMarkedRangesSize());
					}
					break;
				default:
					break;
			}
			dataIoTask.startDataResponse();
		}
		ioTask.setStatus(IoTask.Status.SUCC);
		complete(channel, ioTask);
	}

	@Override
	public final String toString() {
		return String.format(super.toString(), "mock-net");
	}
}
