package com.emc.mongoose.scenario.step.type;

import com.emc.mongoose.model.concurrent.DaemonBase;
import com.emc.mongoose.model.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.github.akurilov.commons.collection.OptLockArrayBuffer;
import com.github.akurilov.commons.collection.OptLockBuffer;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.concurrent.coroutine.Coroutine;
import com.github.akurilov.concurrent.coroutine.CoroutineBase;
import com.github.akurilov.concurrent.throttle.IndexThrottle;
import com.github.akurilov.concurrent.throttle.Throttle;
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
public class BasicLoadGenerator<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadGenerator<I, O> {

	private static final String CLS_NAME = BasicLoadGenerator.class.getSimpleName();

	private volatile boolean recycleQueueFullState = false;
	private volatile boolean itemInputFinishFlag = false;
	private volatile boolean taskInputFinishFlag = false;
	private volatile boolean outputFinishFlag = false;

	private final Input<I> itemInput;
	private final IoTaskBuilder<I, O> ioTaskBuilder;
	private final BlockingQueue<O> recycleQueue;
	private final boolean shuffleFlag;
	private final Lock inputLock = new ReentrantLock();
	private final Random rnd;

	private final LongAdder builtTasksCounter = new LongAdder();
	private final LongAdder recycledTasksCounter = new LongAdder();
	private final LongAdder outputTaskCounter = new LongAdder();
	private final String name;
	private final ThreadLocal<OptLockBuffer<O>> threadLocalTasksBuff;
	private final Coroutine coroutine;

	@SuppressWarnings("unchecked")
	public BasicLoadGenerator(
		final Input<I> itemInput, final IoTaskBuilder<I, O> ioTaskBuilder,
		final Output<O> ioTaskOutput, final Throttle rateThrottle,
		final IndexThrottle weightThrottle, final int batchSize, final long countLimit,
		final SizeInBytes sizeLimit, final int recycleQueueSize, final boolean shuffleFlag
	) {
		this.itemInput = itemInput;
		this.ioTaskBuilder = ioTaskBuilder;
		this.recycleQueue = recycleQueueSize > 0 ?
			new ArrayBlockingQueue<>(recycleQueueSize) : null;
		this.shuffleFlag = shuffleFlag;
		this.rnd = shuffleFlag ? new Random() : null;

		final String ioStr = ioTaskBuilder.getIoType().toString();
		name = Character.toUpperCase(ioStr.charAt(0)) + ioStr.substring(1).toLowerCase() +
			(countLimit > 0 && countLimit < Long.MAX_VALUE ? Long.toString(countLimit) : "") +
			itemInput.toString();

		threadLocalTasksBuff = ThreadLocal.withInitial(() -> new OptLockArrayBuffer<>(batchSize));

		coroutine = new CoroutineBase(ServiceTaskExecutor.INSTANCE) {

			private final long _countLimit;
			{
				if(countLimit > 0) {
					this._countLimit = countLimit;
				} else {
					this._countLimit = Long.MAX_VALUE;
				}
			}
			private final int originIndex = ioTaskBuilder.getOriginIndex();

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
									final long remainingTasksCount = _countLimit -
										getGeneratedTasksCount();
									if(remainingTasksCount > 0) {
										// make the limit not more than batch size
										n = (int) Math.min(remainingTasksCount, n);
										final List<I> items = getItems(itemInput, n);
										if(items == null) {
											itemInputFinishFlag = true;
										} else {
											n = items.size();
											if(n > 0) {
												pendingTasksCount += buildTasks(
													items, tasksBuff, n
												);
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
						// acquire the throttles permit
						n = pendingTasksCount;
						if(weightThrottle != null) {
							n = weightThrottle.tryAcquire(originIndex, n);
						}
						if(rateThrottle != null) {
							n = rateThrottle.tryAcquire(n);
						}
						// try to output
						if(n > 0) {
							if(n == 1) { // single branch
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
									Loggers.MSG.debug(
										"{}: finish due to output's EOF",
										BasicLoadGenerator.this.toString()
									);
									outputFinishFlag = true;
								} catch(final IOException e) {
									LogUtil.exception(
										Level.ERROR, e, "{}: I/O task output failure",
										BasicLoadGenerator.this.toString()
									);
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
									Loggers.MSG.debug(
										"{}: finish due to output's EOF",
										BasicLoadGenerator.this.toString()
									);
									outputFinishFlag = true;
								} catch(final RemoteException e) {
									final Throwable cause = e.getCause();
									if(cause instanceof EOFException) {
										Loggers.MSG.debug(
											"{}: finish due to output's EOF",
											BasicLoadGenerator.this.toString()
										);
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
						LogUtil.exception(Level.ERROR, t, "Unexpected failure");
						t.printStackTrace(System.err);
					}
				} finally {
					if(isFinished()) {
						try {
							BasicLoadGenerator.this.stop();
						} catch(final IllegalStateException ignored) {
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
			protected final void doClose() {
			}
		};
	}

	private List<I> getItems(final Input<I> itemInput, final int n)
	throws IOException {
		// prepare the items buffer
		final List<I> items = new ArrayList<>(n);
		try {
			// get the items from the input
			itemInput.get(items, n);
		} catch(final EOFException e) {
			Loggers.MSG.debug("{}: end of items input", BasicLoadGenerator.this.toString());
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
	protected void doStart()
	throws IllegalStateException {
		try {
			coroutine.start();
		} catch(final RemoteException ignored) {
		}
	}

	@Override
	protected final void doShutdown()
	throws IllegalStateException {
		try {
			coroutine.stop();
		} catch(final RemoteException ignored) {
		}
		Loggers.MSG.debug(
			"{}: generated {}, recycled {}, output {} I/O tasks",
			BasicLoadGenerator.this.toString(), builtTasksCounter.sum(), recycledTasksCounter.sum(),
			outputTaskCounter.sum()
		);
	}

	@Override
	protected final void doStop() {
		shutdown();
	}

	@Override
	protected final void doClose()
	throws IOException {
		if(recycleQueue != null) {
			recycleQueue.clear();
		}
		// the item input may be instantiated by the load generator builder which has no reference
		// to it so the load generator builder should close it
		if(itemInput != null) {
			try {
				inputLock.tryLock(Coroutine.TIMEOUT_NANOS, TimeUnit.NANOSECONDS);
				itemInput.close();
			} catch(final Exception e) {
				LogUtil.exception(Level.WARN, e, "{}: failed to close the item input", toString());
			}
		}
		// I/O task builder is instantiated by the load generator builder which forgets it
		// so the load generator should close it
		ioTaskBuilder.close();
	}
	
	@Override
	public final String toString() {
		return name;
	}
}
