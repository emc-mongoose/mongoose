package com.emc.mongoose.storage.driver.coop;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import com.github.akurilov.commons.collection.OptLockArrayBuffer;
import com.github.akurilov.commons.collection.OptLockBuffer;

import com.github.akurilov.concurrent.coroutine.CoroutinesExecutor;
import com.github.akurilov.concurrent.coroutine.ExclusiveCoroutineBase;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.BlockingQueue;

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
	private final CoopStorageDriverBase<I, O> storageDriver;
	private final OptLockBuffer<O> buff;

	private int n = 0; // the current count of the I/O tasks in the buffer
	private int m;

	public IoTasksDispatchCoroutine(
		final CoroutinesExecutor executor, final CoopStorageDriverBase<I, O> storageDriver,
		final BlockingQueue<O> inTasksQueue, final BlockingQueue<O> childTasksQueue,
		final String stepId, final int batchSize
	) {
		this(
			executor, new OptLockArrayBuffer<>(batchSize), storageDriver, inTasksQueue,
			childTasksQueue, stepId, batchSize
		);
	}

	private IoTasksDispatchCoroutine(
		final CoroutinesExecutor executor, final OptLockBuffer<O> buff,
		final CoopStorageDriverBase<I, O> storageDriver, final BlockingQueue<O> inTasksQueue,
		final BlockingQueue<O> childTasksQueue, final String stepId, final int batchSize
	) {
		super(executor, buff);
		this.storageDriver = storageDriver;
		this.inTasksQueue = inTasksQueue;
		this.childTasksQueue = childTasksQueue;
		this.stepId = stepId;
		this.batchSize = batchSize;
		this.buff = buff;
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {

		ThreadContext.put(KEY_TEST_STEP_ID, stepId);
		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);

		try {
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
				storageDriver.toString(), storageDriver.state()
			);
		}
	}

	@Override
	protected final void doClose() {
		buff.clear();
	}
}
