package com.emc.mongoose.tests.perf.util.mock;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.storage.Credential;
import com.emc.mongoose.storage.driver.nio.base.NioStorageDriverBase;
import com.emc.mongoose.ui.config.Config;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;

/**
 Created by andrey on 11.05.17.
 */
public final class NioStorageDriverMock<I extends Item, O extends IoTask<I>>
extends NioStorageDriverBase<I, O> {

	public NioStorageDriverMock(
		final String jobName, final ContentSource contentSrc, final Config.LoadConfig loadConfig,
		final Config.StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException {
		super(jobName, contentSrc, loadConfig, storageConfig, verifyFlag);
	}

	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {
		return Collections.emptyList();
	}

	@Override
	public final void adjustIoBuffers(final SizeInBytes avgDataItemSize, final IoType ioType)
	throws RemoteException {
	}

	@Override
	protected final String requestNewPath(final String path) {
		return path;
	}

	@Override
	protected final String requestNewAuthToken(final Credential credential) {
		throw new AssertionError("Should not be invoked");
	}

	@Override
	protected final void invokeNio(final O ioTask) {
		ioTask.reset();
		ioTask.startRequest();
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
		ioTask.finishResponse();
		ioTask.setStatus(IoTask.Status.SUCC);
	}

	@Override
	public final String toString() {
		return String.format(super.toString(), "mock-nio");
	}
}
