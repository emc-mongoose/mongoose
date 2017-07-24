package com.emc.mongoose.tests.perf.util.mock;

import com.emc.mongoose.api.common.ByteRange;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 11.05.17.
 */
public final class BasicStorageDriverMock<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O> {

	public BasicStorageDriverMock(
		final String stepName, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException {
		super(stepName, contentSrc, loadConfig, storageConfig, verifyFlag);
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		return true;
	}

	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {
		return Collections.emptyList();
	}

	@Override
	public final void adjustIoBuffers(final long avgTransferSize, final IoType ioType)
	throws RemoteException {
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
	protected final boolean submit(final O ioTask)
	throws InterruptedException {
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
							} catch(final IOException ignored) {
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
		ioTaskCompleted(ioTask);
		return true;
	}

	@Override
	protected final int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException {
		O ioTask;
		for(int i = from; i < to; i ++) {
			ioTask = ioTasks.get(i);
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
								} catch(final IOException ignored) {
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
			ioTaskCompleted(ioTask);
		}
		return to - from;
	}

	@Override
	protected final int submit(final List<O> ioTasks)
	throws InterruptedException {
		return submit(ioTasks, 0, ioTasks.size());
	}

	@Override
	public final String toString() {
		return String.format(super.toString(), "mock-basic");
	}
}
