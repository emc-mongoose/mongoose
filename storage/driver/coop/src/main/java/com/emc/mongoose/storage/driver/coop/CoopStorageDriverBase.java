package com.emc.mongoose.storage.driver.coop;

import com.emc.mongoose.base.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.base.data.DataInput;
import com.emc.mongoose.base.exception.InterruptRunException;
import com.emc.mongoose.base.exception.OmgShootMyFootException;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.composite.CompositeOperation;
import com.emc.mongoose.base.item.op.partial.PartialOperation;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.storage.driver.StorageDriver;
import com.emc.mongoose.storage.driver.StorageDriverBase;
import static com.emc.mongoose.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.Constants.KEY_STEP_ID;

import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.CloseableThreadContext;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public abstract class CoopStorageDriverBase<I extends Item, O extends Operation<I>>
extends StorageDriverBase<I, O>
implements StorageDriver<I, O> {

	protected final Semaphore concurrencyThrottle;
	protected final BlockingQueue<O> childOpQueue;
	private final BlockingQueue<O> inOpQueue;
	private final LongAdder scheduledOpCount = new LongAdder();
	private final LongAdder completedOpCount = new LongAdder();
	private final OperationDispatchTask opDispatchTask;

	protected CoopStorageDriverBase(
		final String testStepId, final DataInput dataInput, final Config storageConfig, final boolean verifyFlag,
		final int batchSize
	) throws OmgShootMyFootException {
		super(testStepId, dataInput, storageConfig, verifyFlag);
		final int inQueueLimit = storageConfig.intVal("driver-limit-queue-input");
		this.childOpQueue = new ArrayBlockingQueue<>(inQueueLimit);
		this.inOpQueue = new ArrayBlockingQueue<>(inQueueLimit);
		this.concurrencyThrottle = new Semaphore(concurrencyLimit > 0 ? concurrencyLimit : Integer.MAX_VALUE, true);
		this.opDispatchTask = new OperationDispatchTask<>(
			ServiceTaskExecutor.INSTANCE, this, inOpQueue, childOpQueue, stepId, batchSize
		);
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		opDispatchTask.start();
	}

	@Override
	public final boolean put(final O op)
	throws InterruptRunException, EOFException {
		if(!isStarted()) {
			throw new EOFException();
		}
		prepare(op);
		if(inOpQueue.offer(op)) {
			scheduledOpCount.increment();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public final int put(final List<O> ops, final int from, final int to)
	throws InterruptRunException, EOFException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int i = from;
		O nextOp;
		while(i < to && isStarted()) {
			nextOp = ops.get(i);
			prepare(nextOp);
			if(inOpQueue.offer(ops.get(i))) {
				i ++;
			} else {
				break;
			}
		}
		final int n = i - from;
		scheduledOpCount.add(n);
		return n;
	}

	@Override
	public final int put(final List<O> ops)
	throws InterruptRunException, EOFException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int n = 0;
		for(final O nextOp: ops) {
			if(isStarted()) {
				prepare(nextOp);
				if(inOpQueue.offer(nextOp)) {
					n ++;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		scheduledOpCount.add(n);
		return n;
	}

	@Override
	public final int activeOpCount() {
		if(concurrencyLimit > 0) {
			return concurrencyLimit - concurrencyThrottle.availablePermits();
		} else {
			return Integer.MAX_VALUE - concurrencyThrottle.availablePermits();
		}
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
		if(concurrencyLimit > 0) {
			return !concurrencyThrottle.hasQueuedThreads()
				&& concurrencyThrottle.availablePermits() >= concurrencyLimit;
		} else {
			return concurrencyThrottle.availablePermits() == Integer.MAX_VALUE;
		}
	}

	protected abstract boolean submit(final O op)
	throws InterruptRunException, IllegalStateException;

	protected abstract int submit(final List<O> ops, final int from, final int to)
	throws InterruptRunException, IllegalStateException;

	protected abstract int submit(final List<O> ops)
	throws InterruptRunException, IllegalStateException;

	@SuppressWarnings("unchecked")
	protected final boolean handleCompleted(final O op) {
		if(super.handleCompleted(op)) {
			completedOpCount.increment();
			if(op instanceof CompositeOperation) {
				final CompositeOperation parentOp = (CompositeOperation) op;
				if(!parentOp.allSubOperationsDone()) {
					final List<O> subOps = parentOp.subOperations();
					for(final O nextSubOp: subOps) {
						if(!childOpQueue.offer(nextSubOp)) {
							Loggers.ERR.warn("{}: Child operations queue overflow, dropping the operation", toString());
							return false;
						}
					}
				}
			} else if(op instanceof PartialOperation) {
				final PartialOperation subOp = (PartialOperation) op;
				final CompositeOperation parentOp = subOp.parent();
				if(parentOp.allSubOperationsDone()) {
					// execute once again to finalize the things if necessary:
					// complete the multipart upload, for example
					if(!childOpQueue.offer((O) parentOp)) {
						Loggers.ERR.warn("{}: Child operations queue overflow, dropping the operation", toString());
						return false;
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void doShutdown() {
		opDispatchTask.stop();
		Loggers.MSG.debug("{}: shut down", toString());
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}

	@Override
	protected void doClose()
	throws InterruptRunException, IOException, IllegalStateException {
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_STEP_ID, stepId)
				.put(KEY_CLASS_NAME, StorageDriverBase.class.getSimpleName())
		) {
			super.doClose();
			opDispatchTask.close();
			childOpQueue.clear();
			inOpQueue.clear();
		}
	}
}
