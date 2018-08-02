package com.emc.mongoose.storage.driver.mock;

import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.op.data.DataOperation;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.storage.driver.StorageDriver;

import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.io.Input;

import com.github.akurilov.confuse.Config;

import java.io.EOFException;
import java.io.IOException;
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
public final class DummyStorageDriverMock<I extends Item, O extends Operation<I>>
extends AsyncRunnableBase
implements StorageDriver<I, O> {

	private final int concurrencyLimit;
	private final BlockingQueue<O> opsResultsQueue;
	private final LongAdder scheduledOpCount = new LongAdder();
	private final LongAdder completedOpCount = new LongAdder();

	public DummyStorageDriverMock(final Config storageConfig) {
		final Config limitConfig = storageConfig.configVal("driver-limit");
		final int outputQueueCapacity = limitConfig.intVal("queue-output");
		this.concurrencyLimit = limitConfig.intVal("concurrency");
		this.opsResultsQueue = new ArrayBlockingQueue<>(outputQueueCapacity);
	}

	@Override
	public final boolean put(final O task)
	throws IOException {
		if(!isStarted()) {
			throw new EOFException();
		}
		checkStateFor(task);
		if(opsResultsQueue.offer(task)) {
			scheduledOpCount.increment();
			completedOpCount.increment();
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
			if(opsResultsQueue.offer(tasks.get(i))) {
				i ++;
			} else {
				break;
			}
		}
		final int n = i - from;
		scheduledOpCount.add(n);
		completedOpCount.add(n);
		return n;
	}

	@Override
	public final int put(final List<O> tasks)
	throws IOException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int n = 0;
		for(final O nextOp: tasks) {
			if(isStarted()) {
				checkStateFor(nextOp);
				if(opsResultsQueue.offer(nextOp)) {
					n ++;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		scheduledOpCount.add(n);
		completedOpCount.add(n);
		return n;
	}

	private void checkStateFor(final O op)
	throws IOException {
		op.reset();
		op.startRequest();
		op.finishRequest();
		op.startResponse();
		if(op instanceof DataOperation) {
			final DataOperation dataOp = (DataOperation) op;
			final DataItem dataItem = dataOp.item();
			switch(dataOp.type()) {
				case CREATE:
					dataOp.countBytesDone(dataItem.size());
					break;
				case READ:
					dataOp.startDataResponse();
				case UPDATE:
					final List<Range> fixedRanges = dataOp.fixedRanges();
					if(fixedRanges == null || fixedRanges.isEmpty()) {
						if(dataOp.hasMarkedRanges()) {
							dataOp.countBytesDone(dataOp.markedRangesSize());
						} else {
							dataOp.countBytesDone(dataItem.size());
						}
					} else {
						dataOp.countBytesDone(dataOp.markedRangesSize());
					}
					break;
				default:
					break;
			}
			dataOp.startDataResponse();
		}
		op.finishResponse();
		op.status(Operation.Status.SUCC);
	}

	@Override
	public final Input<O> getInput()
	throws IOException {
		return this;
	}

	@Override
	public final O get()
	throws EOFException, IOException {
		return opsResultsQueue.poll();
	}

	@Override
	public final int get(final List<O> buffer, final int limit)
	throws IOException {
		return opsResultsQueue.drainTo(buffer, limit);
	}

	@Override
	public final long skip(final long count)
	throws IOException {
		int n = (int) Math.min(count, Integer.MAX_VALUE);
		final List<O> tmpBuff = new ArrayList<>(n);
		n = opsResultsQueue.drainTo(tmpBuff, n);
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
	public final int concurrencyLimit() {
		return concurrencyLimit;
	}

	@Override
	public final int activeOpCount() {
		return (int) (scheduledOpCount() - completedOpCount());
	}

	@Override
	public final long scheduledOpCount() {
		return scheduledOpCount.sum();
	}

	@Override
	public final long completedOpCount() {
		return completedOpCount.sum();
	}

	@Override
	public final boolean isIdle() {
		return opsResultsQueue.isEmpty();
	}

	@Override
	public final void adjustIoBuffers(final long avgTransferSize, final OpType opType) {
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		Loggers.MSG.debug("{}: started", toString());
	}

	@Override
	protected final void doShutdown()
	throws IllegalStateException {
		Loggers.MSG.debug("{}: shut down", toString());
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return true;
	}

	@Override
	protected final void doStop()
	throws IllegalStateException {
		Loggers.MSG.debug("{}: interrupted", toString());
	}

	@Override
	protected final void doClose()
	throws IOException {
		opsResultsQueue.clear();
		Loggers.MSG.debug("{}: closed", toString());
	}

	@Override
	public final String toString() {
		return String.format(super.toString(), "mock-dummy");
	}
}
