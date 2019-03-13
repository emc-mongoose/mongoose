package com.emc.mongoose.storage.driver.coop;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.github.akurilov.commons.collection.CircularArrayBuffer;
import com.github.akurilov.commons.collection.CircularBuffer;

import com.github.akurilov.fiber4j.ExclusiveFiberBase;
import com.github.akurilov.fiber4j.FibersExecutor;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
* Created by andrey on 23.08.17.
*/
public final class OperationDispatchTask<I extends Item, O extends Operation<I>>
				extends ExclusiveFiberBase {

	private static final String CLS_NAME = OperationDispatchTask.class.getSimpleName();

	private final String stepId;
	private final int batchSize;
	private final BlockingQueue<O> childOpQueue;
	private final BlockingQueue<O> inOpQueue;
	private final CoopStorageDriverBase<I, O> storageDriver;
	private final CircularBuffer<O> buff;
	private final Lock buffLock;

	private int n = 0; // the current count of the load operations in the buffer

	public OperationDispatchTask(
					final FibersExecutor executor, final CoopStorageDriverBase<I, O> storageDriver,
					final BlockingQueue<O> inOpQueue, final BlockingQueue<O> childOpQueue, final String stepId,
					final int batchSize) {
		this(
						executor, new CircularArrayBuffer<>(batchSize), new ReentrantLock(), storageDriver, inOpQueue, childOpQueue,
						stepId, batchSize);
	}

	private OperationDispatchTask(
					final FibersExecutor executor, final CircularBuffer<O> buff, final Lock buffLock,
					final CoopStorageDriverBase<I, O> storageDriver, final BlockingQueue<O> inOpQueue,
					final BlockingQueue<O> childOpQueue, final String stepId, final int batchSize) {
		super(executor, buffLock);
		this.buff = buff;
		this.buffLock = buffLock;
		this.storageDriver = storageDriver;
		this.inOpQueue = inOpQueue;
		this.childOpQueue = childOpQueue;
		this.stepId = stepId;
		this.batchSize = batchSize;
	}

	@Override
	protected final void invokeTimedExclusively(final long startTimeNanos) {

		ThreadContext.put(KEY_STEP_ID, stepId);
		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);

		try {
			// child ops go first
			if (n < batchSize) {
				n += childOpQueue.drainTo(buff, batchSize - n);
			}
			// check for the fiber invocation timeout
			if (SOFT_DURATION_LIMIT_NANOS <= System.nanoTime() - startTimeNanos) {
				return;
			}
			// new tasks
			if (n < batchSize) {
				n += inOpQueue.drainTo(buff, batchSize - n);
			}
			// check for the fiber invocation timeout
			if (SOFT_DURATION_LIMIT_NANOS <= System.nanoTime() - startTimeNanos) {
				return;
			}
			// submit the tasks if any
			if (n > 0) {
				if (n == 1) { // non-batch mode
					if (storageDriver.submit(buff.get(0))) {
						buff.clear();
						n--;
					}
				} else { // batch mode
					final int m = storageDriver.submit(buff, 0, n);
					if (m > 0) {
						buff.removeFirst(m);
						n -= m;
					}
				}
			}
		} catch (final IllegalStateException e) {
			LogUtil.exception(
							Level.TRACE, e,
							"{}: failed to submit some load operations due to the illegal storage driver state ({})",
							storageDriver.toString(), storageDriver.state());
		}
	}

	@Override
	protected final void doClose()
					 {
		try {
			if (buffLock.tryLock(WARN_DURATION_LIMIT_NANOS, TimeUnit.NANOSECONDS)) {
				buff.clear();
			} else {
				Loggers.ERR.warn("BufferLock timeout on close");
			}
		} catch (final InterruptedException e) {
			throwUnchecked(e);
		}
	}
}
