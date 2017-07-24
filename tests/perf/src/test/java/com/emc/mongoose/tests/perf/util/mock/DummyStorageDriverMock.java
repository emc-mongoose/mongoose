package com.emc.mongoose.tests.perf.util.mock;

import com.emc.mongoose.api.common.ByteRange;
import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.model.DaemonBase;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.log.Loggers;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by andrey on 11.05.17.
 */
public final class DummyStorageDriverMock<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements StorageDriver<I, O> {

	private final int batchSize;
	private final int queueCapacity;
	private final int concurrencyLevel;
	private final BlockingQueue<O> ioResultsQueue;
	private final LongAdder scheduledTaskCount = new LongAdder();
	private final LongAdder completedTaskCount = new LongAdder();

	public DummyStorageDriverMock(
		final String stepName, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) {
		this.batchSize = loadConfig.getBatchConfig().getSize();
		this.queueCapacity = loadConfig.getQueueConfig().getSize();
		this.concurrencyLevel = storageConfig.getDriverConfig().getConcurrency();
		this.ioResultsQueue = new ArrayBlockingQueue<>(queueCapacity);
	}

	@Override
	public final boolean put(final O task)
	throws IOException {
		if(!isStarted()) {
			throw new EOFException();
		}
		checkStateFor(task);
		if(ioResultsQueue.offer(task)) {
			scheduledTaskCount.increment();
			completedTaskCount.increment();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public final int put(final List<O> tasks, final int from, final int to)
	throws IOException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int i = from;
		O nextTask;
		while(i < to && isStarted()) {
			nextTask = tasks.get(i);
			checkStateFor(nextTask);
			if(ioResultsQueue.offer(tasks.get(i))) {
				i ++;
			} else {
				break;
			}
		}
		final int n = i - from;
		scheduledTaskCount.add(n);
		completedTaskCount.add(n);
		return n;
	}

	@Override
	public final int put(final List<O> tasks)
	throws IOException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int n = 0;
		for(final O nextIoTask : tasks) {
			if(isStarted()) {
				checkStateFor(nextIoTask);
				if(ioResultsQueue.offer(nextIoTask)) {
					n ++;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		scheduledTaskCount.add(n);
		completedTaskCount.add(n);
		return n;
	}

	private void checkStateFor(final O ioTask)
	throws IOException {
		ioTask.reset();
		ioTask.startRequest();
		ioTask.finishRequest();
		ioTask.startResponse();
		if(ioTask instanceof DataIoTask) {
			final DataIoTask dataIoTask = (DataIoTask) ioTask;
			final DataItem dataItem = dataIoTask.getItem();
			switch(dataIoTask.getIoType()) {
				case CREATE:
					dataIoTask.setCountBytesDone(dataItem.size());
					break;
				case READ:
					dataIoTask.startDataResponse();
				case UPDATE:
					final List<ByteRange> fixedByteRanges = dataIoTask.getFixedRanges();
					if(fixedByteRanges == null || fixedByteRanges.isEmpty()) {
						if(dataIoTask.hasMarkedRanges()) {
							dataIoTask.setCountBytesDone(dataIoTask.getMarkedRangesSize());
						} else {
							dataIoTask.setCountBytesDone(dataItem.size());
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
	public final Input<O> getInput()
	throws IOException {
		return this;
	}

	@Override
	public final O get()
	throws EOFException, IOException {
		return ioResultsQueue.poll();
	}

	@Override
	public final List<O> getAll() {
		final List<O> ioTaskResults = new ArrayList<>(queueCapacity);
		ioResultsQueue.drainTo(ioTaskResults, queueCapacity);
		return ioTaskResults;
	}

	@Override
	public final long skip(final long count)
	throws IOException {
		int n = (int) Math.min(count, Integer.MAX_VALUE);
		final List<O> tmpBuff = new ArrayList<>(n);
		n = ioResultsQueue.drainTo(tmpBuff, n);
		tmpBuff.clear();
		return n;
	}

	@Override
	public final List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {
		return Collections.emptyList();
	}

	@Override
	public final int getConcurrencyLevel()
	throws RemoteException {
		return concurrencyLevel;
	}

	@Override
	public final int getActiveTaskCount()
	throws RemoteException {
		return (int) (getScheduledTaskCount() - getCompletedTaskCount());
	}

	@Override
	public final long getScheduledTaskCount()
	throws RemoteException {
		return scheduledTaskCount.sum();
	}

	@Override
	public final long getCompletedTaskCount()
	throws RemoteException {
		return completedTaskCount.sum();
	}

	@Override
	public final boolean isIdle()
	throws RemoteException {
		return ioResultsQueue.isEmpty();
	}

	@Override
	public final void adjustIoBuffers(final long avgTransferSize, final IoType ioType)
	throws RemoteException {
	}

	@Override
	protected final void doShutdown()
	throws IllegalStateException {
		Loggers.MSG.debug("{}: shut down", toString());
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		return true;
	}

	@Override
	protected final void doInterrupt()
	throws IllegalStateException {
		Loggers.MSG.debug("{}: interrupted", toString());
	}

	@Override
	protected final void doClose()
	throws IOException {
		super.doClose();
		ioResultsQueue.clear();
		Loggers.MSG.debug("{}: closed", toString());
	}

	@Override
	public final String toString() {
		return String.format(super.toString(), "mock-dummy");
	}
}
