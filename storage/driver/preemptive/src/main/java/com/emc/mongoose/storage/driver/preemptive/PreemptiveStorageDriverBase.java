package com.emc.mongoose.storage.driver.preemptive;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.log.Loggers;

import java.io.EOFException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class PreemptiveStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements PreemptiveStorageDriver<I,O> {

	private final ThreadPoolExecutor ioExecutor;

	protected PreemptiveStorageDriverBase(
		final String stepId, final DataInput itemDataInput, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {
		super(stepId, itemDataInput, loadConfig, storageConfig, verifyFlag);
		final int inQueueSize = storageConfig.getDriverConfig().getQueueConfig().getInput();
		ioExecutor = new ThreadPoolExecutor(
			ioWorkerCount, ioWorkerCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(inQueueSize),
			new LogContextThreadFactory("io-executor-" + stepId, true)
		);
	}

	@Override
	public final boolean put(final O ioTask)
	throws EOFException {
		try {
			ioExecutor.execute(blockingIoTask(ioTask));
			return true;
		} catch(final RejectedExecutionException e) {
			if(!isStarted() || ioExecutor.isShutdown() || ioExecutor.isTerminated()) {
				throw new EOFException();
			}
			return false;
		}
	}

	@Override
	public final int put(final List<O> ioTasks, final int from, final int to)
	throws EOFException {
		if(!isStarted() || ioExecutor.isShutdown() || ioExecutor.isTerminated()) {
			throw new EOFException();
		}
		int i = from;
		try {
			while(i < to) {
				ioExecutor.execute(blockingIoTask(ioTasks.get(i)));
				i ++;
			}
		} catch(final RejectedExecutionException ignored) {
		}
		return to - i;
	}

	@Override
	public final int put(final List<O> ioTasks)
	throws EOFException {
		return put(ioTasks, 0, ioTasks.size());
	}

	private Runnable blockingIoTask(final O ioTask) {
		prepareIoTask(ioTask);
		return () -> {
			execute(ioTask);
			ioTaskCompleted(ioTask);
		};
	}

	protected abstract void execute(final O ioTask);

	@Override
	public final int getActiveTaskCount() {
		return ioExecutor.getActiveCount();
	}

	@Override
	public final long getScheduledTaskCount() {
		return ioExecutor.getTaskCount();
	}

	@Override
	public final long getCompletedTaskCount() {
		return ioExecutor.getCompletedTaskCount();
	}

	@Override
	public final boolean isIdle() {
		return ioExecutor.getActiveCount() == 0;
	}

	@Override
	protected void doStart() {
		ioExecutor.prestartAllCoreThreads();
		Loggers.MSG.debug("{}: started", toString());
	}

	@Override
	protected void doShutdown() {
		ioExecutor.shutdown();
		Loggers.MSG.debug("{}: shut down", toString());
	}

	@Override
	protected void doInterrupt() {
		final List<Runnable> tasks = ioExecutor.shutdownNow();
		Loggers.MSG.debug("{}: interrupted, {} active tasks remain", toString(), tasks.size());
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return ioExecutor.awaitTermination(timeout, timeUnit);
	}
}
