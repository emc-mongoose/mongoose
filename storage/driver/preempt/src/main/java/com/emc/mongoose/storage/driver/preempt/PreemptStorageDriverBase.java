package com.emc.mongoose.storage.driver.preempt;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.storage.driver.StorageDriver;
import com.emc.mongoose.storage.driver.StorageDriverBase;
import com.github.akurilov.confuse.Config;

import java.io.EOFException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class PreemptStorageDriverBase<I extends Item, O extends Operation<I>>
extends StorageDriverBase<I, O>
implements StorageDriver<I,O> {

	private final ThreadPoolExecutor ioExecutor;

	protected PreemptStorageDriverBase(
		final String stepId, final DataInput itemDataInput, final Config storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {
		super(stepId, itemDataInput, storageConfig, verifyFlag);
		if(ioWorkerCount != concurrencyLimit) {
			throw new IllegalArgumentException(
				"Storage driver I/O worker count (" + ioWorkerCount + ") should be equal to the "
					+ " concurrency limit (" + concurrencyLimit + ")"
			);
		}
		final int inQueueSize = storageConfig.intVal("driver-limit-queue-input");
		ioExecutor = new ThreadPoolExecutor(
			ioWorkerCount, ioWorkerCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(inQueueSize),
			new LogContextThreadFactory("io-executor-" + stepId, true)
		);
	}

	@Override
	public final boolean put(final O op)
	throws InterruptRunException, EOFException {
		try {
			ioExecutor.execute(wrapBlockingOperation(op));
			return true;
		} catch(final RejectedExecutionException e) {
			if(!isStarted() || ioExecutor.isShutdown() || ioExecutor.isTerminated()) {
				throw new EOFException();
			}
			return false;
		}
	}

	@Override
	public final int put(final List<O> ops, final int from, final int to)
	throws InterruptRunException, EOFException {
		if(!isStarted() || ioExecutor.isShutdown() || ioExecutor.isTerminated()) {
			throw new EOFException();
		}
		int i = from;
		try {
			while(i < to) {
				ioExecutor.execute(wrapBlockingOperation(ops.get(i)));
				i ++;
			}
		} catch(final RejectedExecutionException ignored) {
		}
		return i - from;
	}

	@Override
	public final int put(final List<O> ops)
	throws InterruptRunException, EOFException {
		return put(ops, 0, ops.size());
	}

	private Runnable wrapBlockingOperation(final O op)
	throws InterruptRunException {
		prepareOperation(op);
		return () -> {
			execute(op);
			opCompleted(op);
		};
	}

	protected abstract void execute(final O op);

	@Override
	public final int activeOpCount() {
		return ioExecutor.getActiveCount();
	}

	@Override
	public final long scheduledOpCount() {
		return ioExecutor.getTaskCount();
	}

	@Override
	public final long completedOpCount() {
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
		// prevent enqueuing new load operations
		ioExecutor.shutdown();
		// drop all pending load operations
		ioExecutor.getQueue().clear();
		Loggers.MSG.debug("{}: shut down", toString());
	}

	@Override
	protected void doStop()
	throws InterruptRunException {
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
			ioExecutor.shutdownNow();
			throw new InterruptRunException(e);
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
