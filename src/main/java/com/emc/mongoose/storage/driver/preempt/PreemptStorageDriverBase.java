package com.emc.mongoose.storage.driver.preempt;

import com.emc.mongoose.model.exception.OmgShootMyFootException;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.model.data.DataInput;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.storage.driver.StorageDriverBase;
import com.emc.mongoose.config.load.LoadConfig;
import com.emc.mongoose.config.storage.StorageConfig;
import com.emc.mongoose.logging.Loggers;

import java.io.EOFException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class PreemptStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements StorageDriver<I,O> {

	private final ThreadPoolExecutor ioExecutor;

	protected PreemptStorageDriverBase(
		final String stepId, final DataInput itemDataInput, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {
		super(stepId, itemDataInput, loadConfig, storageConfig, verifyFlag);
		if(ioWorkerCount != concurrencyLevel) {
			throw new IllegalArgumentException(
				"Storage driver I/O worker count (" + ioWorkerCount + ") should be equal to the "
					+ " concurrency limit (" + concurrencyLevel + ")"
			);
		}
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
		return i - from;
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
		// prevent enqueuing new I/O tasks
		ioExecutor.shutdown();
		// drop all pending I/O tasks
		ioExecutor.getQueue().clear();
		Loggers.MSG.debug("{}: shut down", toString());
	}

	@Override
	protected void doStop() {
		Loggers.MSG.debug("{}: interrupting...", toString());
		try {
			if(ioExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				Loggers.MSG.debug("{}: interrupting finished in 1 seconds", toString());
			} else {
				Loggers.ERR.debug(
					"{}: interrupting did not finish in 1 second, forcing", toString()
				);
			}
		} catch(final InterruptedException e) {
			throw new CancellationException(e.getMessage());
		} finally {
			Loggers.MSG.debug("{}: interrupted", toString());
		}
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return ioExecutor.awaitTermination(timeout, timeUnit);
	}
}
