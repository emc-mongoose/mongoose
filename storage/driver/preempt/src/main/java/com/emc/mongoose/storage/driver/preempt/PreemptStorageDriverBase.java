package com.emc.mongoose.storage.driver.preempt;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.config.IllegalConfigurationException;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.Operation.Status;
import com.emc.mongoose.base.logging.LogContextThreadFactory;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.storage.driver.StorageDriver;
import com.emc.mongoose.base.storage.driver.StorageDriverBase;
import com.github.akurilov.confuse.Config;
import java.io.EOFException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class PreemptStorageDriverBase<I extends Item, O extends Operation<I>>
				extends StorageDriverBase<I, O> implements StorageDriver<I, O> {

	private final ThreadPoolExecutor ioExecutor;

	protected PreemptStorageDriverBase(
					final String stepId,
					final DataInput itemDataInput,
					final Config storageConfig,
					final boolean verifyFlag)
					throws IllegalConfigurationException {
		super(stepId, itemDataInput, storageConfig, verifyFlag);
		if (ioWorkerCount != concurrencyLimit) {
			throw new IllegalArgumentException(
							"Storage driver I/O worker count ("
											+ ioWorkerCount
											+ ") should be equal to the "
											+ " concurrency limit ("
											+ concurrencyLimit
											+ ")");
		}
		final int inQueueSize = storageConfig.intVal("driver-limit-queue-input");
		ioExecutor = new ThreadPoolExecutor(
						ioWorkerCount,
						ioWorkerCount,
						0,
						TimeUnit.SECONDS,
						new ArrayBlockingQueue<>(inQueueSize),
						new LogContextThreadFactory("io-executor-" + stepId, true));
	}

	@Override
	public final boolean put(final O op)  {
		try {
			ioExecutor.execute(wrapToBlocking(op));
			return true;
		} catch (final RejectedExecutionException e) {
			if (!isStarted() || ioExecutor.isShutdown() || ioExecutor.isTerminated()) {
				throwUnchecked(new EOFException());
			}
			return false;
		}
	}

	@Override
	public final int put(final List<O> ops, final int from, final int to)
					 {
		if (!isStarted() || ioExecutor.isShutdown() || ioExecutor.isTerminated()) {
			throwUnchecked(new EOFException());
		}
		int i = from;
		try {
			while (i < to) {
				ioExecutor.execute(wrapToBlocking(ops.get(i)));
				i++;
			}
		} catch (final RejectedExecutionException ignored) {}
		return i - from;
	}

	@Override
	public final int put(final List<O> ops)  {
		return put(ops, 0, ops.size());
	}

	private Runnable wrapToBlocking(final O op)  {
		if (prepare(op)) {
			return () -> {
				execute(op);
				handleCompleted(op);
			};
		} else {
			return () -> {
				op.status(Status.FAIL_UNKNOWN);
				handleCompleted(op);
			};
		}
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
	protected void doStop()  {
		Loggers.MSG.debug("{}: interrupting...", toString());
		try {
			if (ioExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				Loggers.MSG.debug("{}: interrupting finished in 1 seconds", toString());
			} else {
				Loggers.ERR.debug("{}: interrupting did not finish in 1 second, forcing", toString());
			}
		} catch (final InterruptedException e) {
			ioExecutor.shutdownNow();
			throwUnchecked(e);
		} finally {
			Loggers.MSG.debug("{}: interrupted", toString());
		}
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		return ioExecutor.awaitTermination(timeout, timeUnit);
	}
}
