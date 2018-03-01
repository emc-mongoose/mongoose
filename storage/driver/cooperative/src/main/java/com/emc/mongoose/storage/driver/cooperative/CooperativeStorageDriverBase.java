package com.emc.mongoose.storage.driver.cooperative;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.api.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.driver.queue.QueueConfig;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import org.apache.logging.log4j.CloseableThreadContext;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public abstract class CooperativeStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements CooperativeStorageDriver<I, O> {

	protected final Semaphore concurrencyThrottle;
	protected final BlockingQueue<O> childTasksQueue;
	private final BlockingQueue<O> inTasksQueue;
	private final LongAdder scheduledTaskCount = new LongAdder();
	private final LongAdder completedTaskCount = new LongAdder();
	private final IoTasksDispatchCoroutine ioTasksDispatchCoroutine;

	protected CooperativeStorageDriverBase(
		final String testStepId, final DataInput dataInput, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {
		super(testStepId, dataInput, loadConfig, storageConfig, verifyFlag);
		final QueueConfig queueConfig = storageConfig.getDriverConfig().getQueueConfig();
		this.childTasksQueue = new ArrayBlockingQueue<>(queueConfig.getInput());
		this.inTasksQueue = new ArrayBlockingQueue<>(queueConfig.getInput());
		if(concurrencyLevel > 0) {
			this.concurrencyThrottle = new Semaphore(concurrencyLevel, true);
		} else {
			this.concurrencyThrottle = new Semaphore(Integer.MAX_VALUE, false);
		}
		final int batchSize = loadConfig.getBatchConfig().getSize();
		this.ioTasksDispatchCoroutine = new IoTasksDispatchCoroutine<>(
			SVC_EXECUTOR, this, inTasksQueue, childTasksQueue, stepId, batchSize
		);
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		ioTasksDispatchCoroutine.start();
	}

	@Override
	public final boolean put(final O task)
	throws EOFException {
		if(!isStarted()) {
			throw new EOFException();
		}
		prepareIoTask(task);
		if(inTasksQueue.offer(task)) {
			scheduledTaskCount.increment();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public final int put(final List<O> tasks, final int from, final int to)
	throws EOFException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int i = from;
		O nextTask;
		while(i < to && isStarted()) {
			nextTask = tasks.get(i);
			prepareIoTask(nextTask);
			if(inTasksQueue.offer(tasks.get(i))) {
				i ++;
			} else {
				break;
			}
		}
		final int n = i - from;
		scheduledTaskCount.add(n);
		return n;
	}

	@Override
	public final int put(final List<O> tasks)
	throws EOFException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int n = 0;
		for(final O nextIoTask : tasks) {
			if(isStarted()) {
				prepareIoTask(nextIoTask);
				if(inTasksQueue.offer(nextIoTask)) {
					n ++;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		scheduledTaskCount.add(n);
		return n;
	}

	@Override
	public final int getActiveTaskCount() {
		if(concurrencyLevel > 0) {
			return concurrencyLevel - concurrencyThrottle.availablePermits();
		} else {
			return Integer.MAX_VALUE - concurrencyThrottle.availablePermits();
		}
	}

	@Override
	public final long getScheduledTaskCount() {
		return scheduledTaskCount.sum();
	}

	@Override
	public final long getCompletedTaskCount() {
		return completedTaskCount.sum();
	}

	@Override
	public final boolean isIdle() {
		if(concurrencyLevel > 0) {
			return !concurrencyThrottle.hasQueuedThreads() &&
				concurrencyThrottle.availablePermits() >= concurrencyLevel;
		} else {
			return concurrencyThrottle.availablePermits() == Integer.MAX_VALUE;
		}
	}

	protected abstract boolean submit(final O ioTask)
	throws IllegalStateException;

	protected abstract int submit(final List<O> ioTasks, final int from, final int to)
	throws IllegalStateException;

	protected abstract int submit(final List<O> ioTasks)
	throws IllegalStateException;

	@SuppressWarnings("unchecked")
	protected final void ioTaskCompleted(final O ioTask) {

		super.ioTaskCompleted(ioTask);

		completedTaskCount.increment();

		if(ioTask instanceof CompositeIoTask) {
			final CompositeIoTask parentTask = (CompositeIoTask) ioTask;
			if(!parentTask.allSubTasksDone()) {
				final List<O> subTasks = parentTask.getSubTasks();
				for(final O nextSubTask : subTasks) {
					if(!childTasksQueue.offer(nextSubTask/*, 1, TimeUnit.MICROSECONDS*/)) {
						Loggers.ERR.warn(
							"{}: I/O child tasks queue overflow, dropping the I/O sub-task",
							toString()
						);
						break;
					}
				}
			}
		} else if(ioTask instanceof PartialIoTask) {
			final PartialIoTask subTask = (PartialIoTask) ioTask;
			final CompositeIoTask parentTask = subTask.getParent();
			if(parentTask.allSubTasksDone()) {
				// execute once again to finalize the things if necessary:
				// complete the multipart upload, for example
				if(!childTasksQueue.offer((O) parentTask/*, 1, TimeUnit.MICROSECONDS*/)) {
					Loggers.ERR.warn(
						"{}: I/O child tasks queue overflow, dropping the I/O task", toString()
					);
				}
			}
		}
	}

	@Override
	protected void doShutdown() {
		ioTasksDispatchCoroutine.stop();
		Loggers.MSG.debug("{}: shut down", toString());
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}

	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, StorageDriverBase.class.getSimpleName())
		) {
			super.doClose();
			ioTasksDispatchCoroutine.close();
			childTasksQueue.clear();
			inTasksQueue.clear();
		}
	}
}
