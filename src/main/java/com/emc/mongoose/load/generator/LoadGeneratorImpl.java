package com.emc.mongoose.load.generator;

import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.io.task.IoTaskBuilder;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.commons.collection.OptLockArrayBuffer;
import com.github.akurilov.commons.collection.OptLockBuffer;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.commons.concurrent.throttle.IndexThrottle;
import com.github.akurilov.commons.concurrent.throttle.Throttle;

import com.github.akurilov.fiber4j.Fiber;
import com.github.akurilov.fiber4j.FiberBase;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.emc.mongoose.Constants.KEY_CLASS_NAME;

/**
 Created by kurila on 11.07.16.
 */
public class LoadGeneratorImpl<I extends Item, O extends IoTask<I>>
extends FiberBase
implements LoadGenerator<I, O> {

	private static final String CLS_NAME = LoadGeneratorImpl.class.getSimpleName();

	private volatile boolean recycleQueueFullState = false;
	private volatile boolean itemInputFinishFlag = false;
	private volatile boolean taskInputFinishFlag = false;
	private volatile boolean outputFinishFlag = false;

	private final Input<I> itemInput;
	private final IoTaskBuilder<I, O> ioTaskBuilder;
	private final int originIndex;
	private final Object[] throttles;
	private final Output<O> ioTaskOutput;
	private final Lock inputLock = new ReentrantLock();
	private final int batchSize;
	private final long countLimit;
	private final BlockingQueue<O> recycleQueue;
	private final boolean shuffleFlag;
	private final Random rnd;
	private final String name;
	private final ThreadLocal<OptLockBuffer<O>> threadLocalTasksBuff;
	private final LongAdder builtTasksCounter = new LongAdder();
	private final LongAdder recycledTasksCounter = new LongAdder();
	private final LongAdder outputTaskCounter = new LongAdder();

	@SuppressWarnings("unchecked")
	public LoadGeneratorImpl(
		final Input<I> itemInput, final IoTaskBuilder<I, O> ioTaskBuilder, final List<Object> throttles,
		final Output<O> ioTaskOutput, final int batchSize, final long countLimit, final int recycleQueueSize,
		final boolean shuffleFlag
	) {

		super(ServiceTaskExecutor.INSTANCE);

		this.itemInput = itemInput;
		this.ioTaskBuilder = ioTaskBuilder;
		this.originIndex = ioTaskBuilder.getOriginIndex();
		this.throttles = throttles.toArray(new Object[] {});
		this.ioTaskOutput = ioTaskOutput;
		this.batchSize = batchSize;
		this.countLimit = countLimit > 0 ? countLimit : Long.MAX_VALUE;
		this.recycleQueue = recycleQueueSize > 0 ? new ArrayBlockingQueue<>(recycleQueueSize) : null;
		this.shuffleFlag = shuffleFlag;
		this.rnd = shuffleFlag ? new Random() : null;
		final String ioStr = ioTaskBuilder.getIoType().toString();
		name = Character.toUpperCase(ioStr.charAt(0)) + ioStr.substring(1).toLowerCase() +
			(countLimit > 0 && countLimit < Long.MAX_VALUE ? Long.toString(countLimit) : "") +
			itemInput.toString();
		threadLocalTasksBuff = ThreadLocal.withInitial(() -> new OptLockArrayBuffer<>(batchSize));
	}

	@Override
	protected final void invokeTimed(final long startTimeNanos) {

		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);
		final OptLockBuffer<O> tasksBuff = threadLocalTasksBuff.get();
		int pendingTasksCount = tasksBuff.size();
		int n = batchSize - pendingTasksCount;

		try {

			if(n > 0) { // the tasks buffer has free space for the new tasks
				if(!itemInputFinishFlag) {
					// try to produce new items from the items input
					if(inputLock.tryLock()) {
						try {
							// find the remaining count of the tasks to generate
							final long remainingTasksCount = countLimit - getGeneratedTasksCount();
							if(remainingTasksCount > 0) {
								// make the limit not more than batch size
								n = (int) Math.min(remainingTasksCount, n);
								final List<I> items = getItems(itemInput, n);
								if(items == null) {
									itemInputFinishFlag = true;
								} else {
									n = items.size();
									if(n > 0) {
										pendingTasksCount += buildTasks(items, tasksBuff, n);
									}
								}
							}
						} finally {
							inputLock.unlock();
						}
					}
				} else { // items input was exhausted
					if(recycleQueue == null) { // recycling is disabled
						taskInputFinishFlag = true; // allow shutdown
					} else { // recycle the tasks if any
						n = recycleQueue.drainTo(tasksBuff, n);
						if(n > 0) {
							pendingTasksCount += n;
							recycledTasksCounter.add(n);
						}
					}
				}
			}

			if(pendingTasksCount > 0) {
				n = pendingTasksCount;
				// acquire the permit for all the throttles
				for(int i = 0; i < throttles.length; i ++) {
					final Object throttle = throttles[i];
					if(throttle instanceof Throttle) {
						n = ((Throttle) throttle).tryAcquire(n);
					} else if(throttle instanceof IndexThrottle) {
						n = ((IndexThrottle) throttle).tryAcquire(originIndex, n);
					} else {
						throw new AssertionError("Unexpected throttle type: " + throttle.getClass());
					}
				}
				// try to output
				if(n > 0) {
					if(n == 1) { // single mode branch
						try {
							final O task = tasksBuff.get(0);
							if(ioTaskOutput.put(task)) {
								outputTaskCounter.increment();
								if(pendingTasksCount == 1) {
									tasksBuff.clear();
								} else {
									tasksBuff.remove(0);
								}
							}
						} catch(final EOFException e) {
							Loggers.MSG.debug("{}: finish due to output's EOF", name);
							outputFinishFlag = true;
						} catch(final IOException e) {
							LogUtil.exception(Level.ERROR, e, "{}: I/O task output failure", name);
						}
					} else { // batch mode branch
						try {
							n = ioTaskOutput.put(tasksBuff, 0, n);
							outputTaskCounter.add(n);
							if(n < pendingTasksCount) {
								tasksBuff.removeRange(0, n);
							} else {
								tasksBuff.clear();
							}
						} catch(final EOFException e) {
							Loggers.MSG.debug("{}: finish due to output's EOF", name);
							outputFinishFlag = true;
						} catch(final RemoteException e) {
							final Throwable cause = e.getCause();
							if(cause instanceof EOFException) {
								Loggers.MSG.debug("{}: finish due to output's EOF", name);
								outputFinishFlag = true;
							} else {
								LogUtil.exception(Level.ERROR, cause, "Unexpected failure");
								e.printStackTrace(System.err);
							}
						}
					}
				}
			}

		} catch(final Throwable t) {
			if(!(t instanceof EOFException)) {
				LogUtil.exception(Level.ERROR, t, "{}: unexpected failure", name);
				t.printStackTrace(System.err);
			}
		} finally {
			if(isFinished()) {
				try {
					stop();
				} catch(final IllegalStateException ignored) {
				}
			}
		}
	}

	private List<I> getItems(final Input<I> itemInput, final int n)
	throws IOException {
		// prepare the items buffer
		final List<I> items = new ArrayList<>(n);
		try {
			// get the items from the input
			itemInput.get(items, n);
		} catch(final EOFException e) {
			Loggers.MSG.debug("{}: end of items input", LoadGeneratorImpl.this.toString());
			return null;
		}
		return items;
	}

	// build new tasks for the corresponding items
	private long buildTasks(final List<I> items, final OptLockBuffer<O> tasksBuff, final int n)
	throws IOException {
		if(shuffleFlag) {
			Collections.shuffle(items, rnd);
		}
		try {
			ioTaskBuilder.getInstances(items, tasksBuff);
			builtTasksCounter.add(n);
			return n;
		} catch(final IllegalArgumentException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to generate the I/O task");
		}
		return 0;
	}

	@Override
	public final long getGeneratedTasksCount() {
		return builtTasksCounter.sum() + recycledTasksCounter.sum();
	}

	@Override
	public final IoType getIoType() {
		return ioTaskBuilder.getIoType();
	}

	@Override
	public final boolean isRecycling() {
		return recycleQueue != null;
	}

	@Override
	public final void recycle(final O ioTask) {
		if(recycleQueue != null) {
			if(!recycleQueue.offer(ioTask)) {
				if(!recycleQueueFullState && 0 == recycleQueue.remainingCapacity()) {
					recycleQueueFullState = true;
					Loggers.ERR.error("{}: cannot recycle I/O tasks, queue is full", name);
				}
			}
		}
	}

	@Override
	public final boolean isFinished() {
		return outputFinishFlag ||
			itemInputFinishFlag && taskInputFinishFlag &&
				getGeneratedTasksCount() == outputTaskCounter.sum();
	}

	@Override
	protected final void doShutdown()
	throws IllegalStateException {
		stop();
		Loggers.MSG.debug(
			"{}: generated {}, recycled {}, output {} I/O tasks",
			LoadGeneratorImpl.this.toString(), builtTasksCounter.sum(), recycledTasksCounter.sum(),
			outputTaskCounter.sum()
		);
	}

	@Override
	protected final void doClose()
	throws IOException {
		if(recycleQueue != null) {
			recycleQueue.clear();
		}
		// the item input may be instantiated by the load generator builder which has no reference to it so the load
		// generator builder should close it
		if(itemInput != null) {
			try {
				inputLock.tryLock(Fiber.TIMEOUT_NANOS, TimeUnit.NANOSECONDS);
				itemInput.close();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "{}: failed to close the item input", toString());
			}
		}
		// I/O task builder is instantiated by the load generator builder which forgets it so the load generator should
		// close it
		ioTaskBuilder.close();
	}

	@Override
	public final String toString() {
		return name;
	}
}
