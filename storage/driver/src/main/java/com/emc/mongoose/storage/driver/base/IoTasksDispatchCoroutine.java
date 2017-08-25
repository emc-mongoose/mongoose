package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.api.model.concurrent.ThreadDump;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import com.github.akurilov.commons.collection.OptLockArrayBuffer;
import com.github.akurilov.commons.collection.OptLockBuffer;
import com.github.akurilov.coroutines.CoroutinesProcessor;
import com.github.akurilov.coroutines.ExclusiveCoroutineBase;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 23.08.17.
 */
public final class IoTasksDispatchCoroutine<I extends Item, O extends IoTask<I>>
extends ExclusiveCoroutineBase {

	private static final String CLS_NAME = IoTasksDispatchCoroutine.class.getSimpleName();

	private final String stepId;
	private final int batchSize;
	private final BlockingQueue<O> childTasksQueue;
	private final BlockingQueue<O> inTasksQueue;
	private final StorageDriverBase<I, O> storageDriver;
	private final OptLockBuffer<O> buff;

	private int n = 0; // the current count of the I/O tasks in the buffer
	private int m;

	public IoTasksDispatchCoroutine(
		final CoroutinesProcessor coroutinesProcessor, final StorageDriverBase<I, O> storageDriver,
		final BlockingQueue<O> inTasksQueue, final BlockingQueue<O> childTasksQueue,
		final String stepId, final int batchSize
	) {
		this(
			coroutinesProcessor, new OptLockArrayBuffer<>(batchSize), storageDriver, inTasksQueue,
			childTasksQueue, stepId, batchSize
		);
	}

	private IoTasksDispatchCoroutine(
		final CoroutinesProcessor coroutinesProcessor, final OptLockBuffer<O> buff,
		final StorageDriverBase<I, O> storageDriver, final BlockingQueue<O> inTasksQueue,
		final BlockingQueue<O> childTasksQueue, final String stepId, final int batchSize
	) {
		super(coroutinesProcessor, buff);
		this.storageDriver = storageDriver;
		this.inTasksQueue = inTasksQueue;
		this.childTasksQueue = childTasksQueue;
		this.stepId = stepId;
		this.batchSize = batchSize;
		this.buff = buff;
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, CLS_NAME)
		) {
			// child tasks go first
			if(n < batchSize) {
				n += childTasksQueue.drainTo(buff, batchSize - n);
			}
			// check for the coroutine invocation timeout
			if(TIMEOUT_NANOS <= System.nanoTime() - startTimeNanos) {
				return;
			}
			// new tasks
			if(n < batchSize) {
				n += inTasksQueue.drainTo(buff, batchSize - n);
			}
			// check for the coroutine invocation timeout
			if(TIMEOUT_NANOS <= System.nanoTime() - startTimeNanos) {
				return;
			}
			// submit the tasks if any
			if(n > 0) {
				if(n == 1) { // non-batch mode
					if(storageDriver.submit(buff.get(0))) {
						buff.clear();
						n --;
					}
				} else { // batch mode
					m = storageDriver.submit(buff, 0, n);
					if(m > 0) {
						buff.removeRange(0, m);
						n -= m;
					}
				}
			}
		} catch(final IllegalStateException e) {
			LogUtil.exception(
				Level.DEBUG, e, "{}: failed to submit some I/O tasks due to the illegal " +
					"storage driver state ({})",
				storageDriver.toString(), storageDriver.getState()
			);
		}
	}

	@Override
	protected final void doClose() {
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, getClass().getSimpleName())
		) {
			if(!buff.tryLock(TIMEOUT_NANOS, TimeUnit.NANOSECONDS)) {
				Loggers.ERR.debug(
					"{}: failed to obtain the I/O tasks buffer lock in time, thread dump:\n",
					storageDriver.toString(), new ThreadDump().toString()
				);
			}
			buff.clear();
		} catch(final InterruptedException e) {
			LogUtil.exception(
				Level.WARN, e, "{}: interrupted on close", storageDriver.toString()
			);
		}
	}
}
