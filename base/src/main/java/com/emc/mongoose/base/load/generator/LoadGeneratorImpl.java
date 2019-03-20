package com.emc.mongoose.base.load.generator;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Exceptions.throwUncheckedIfInterrupted;
import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.OperationsBuilder;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.github.akurilov.commons.collection.CircularArrayBuffer;
import com.github.akurilov.commons.collection.CircularBuffer;
import com.github.akurilov.commons.concurrent.throttle.IndexThrottle;
import com.github.akurilov.commons.concurrent.throttle.Throttle;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.fiber4j.Fiber;
import com.github.akurilov.fiber4j.FiberBase;
import java.io.EOFException;
import java.io.IOException;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

/** Created by kurila on 11.07.16. */
public class LoadGeneratorImpl<I extends Item, O extends Operation<I>> extends FiberBase
				implements LoadGenerator<I, O> {

	private static final String CLS_NAME = LoadGeneratorImpl.class.getSimpleName();

	private volatile boolean recycleQueueFullState = false;
	private volatile boolean itemInputFinishFlag = false;
	private volatile boolean opInputFinishFlag = false;
	private volatile boolean outputFinishFlag = false;

	private final Input<I> itemInput;
	private final OperationsBuilder<I, O> opsBuilder;
	private final int originIndex;
	private final Object[] throttles;
	private final Output<O> opOutput;
	private final Lock inputLock = new ReentrantLock();
	private final int batchSize;
	private final long countLimit;
	private final BlockingQueue<O> recycleQueue;
	private final boolean recycleFlag;
	private final boolean shuffleFlag;
	private final Random rnd;
	private final String name;
	private final ThreadLocal<CircularBuffer<O>> threadLocalOpBuff;
	private final LongAdder builtTasksCounter = new LongAdder();
	private final LongAdder recycledOpCounter = new LongAdder();
	private final LongAdder outputOpCounter = new LongAdder();

	@SuppressWarnings("unchecked")
	public LoadGeneratorImpl(
					final Input<I> itemInput,
					final OperationsBuilder<I, O> opsBuilder,
					final List<Object> throttles,
					final Output<O> opOutput,
					final int batchSize,
					final long countLimit,
					final int recycleQueueSize,
					final boolean recycleFlag,
					final boolean shuffleFlag) {
		super(ServiceTaskExecutor.INSTANCE);
		this.itemInput = itemInput;
		this.opsBuilder = opsBuilder;
		this.originIndex = opsBuilder.originIndex();
		this.throttles = throttles.toArray(new Object[]{});
		this.opOutput = opOutput;
		this.batchSize = batchSize;
		this.countLimit = countLimit > 0 ? countLimit : Long.MAX_VALUE;
		this.recycleQueue = new ArrayBlockingQueue<>(recycleQueueSize, true);
		this.recycleFlag = recycleFlag;
		this.shuffleFlag = shuffleFlag;
		this.rnd = shuffleFlag ? new Random() : null;
		final var ioStr = opsBuilder.opType().toString();
		name = Character.toUpperCase(ioStr.charAt(0))
						+ ioStr.substring(1).toLowerCase()
						+ (countLimit > 0 && countLimit < Long.MAX_VALUE ? Long.toString(countLimit) : "")
						+ itemInput.toString();
		threadLocalOpBuff = ThreadLocal.withInitial(() -> new CircularArrayBuffer<>(batchSize));
	}

	@Override
	protected final void invokeTimed(final long startTimeNanos) {

		ThreadContext.put(KEY_CLASS_NAME, CLS_NAME);
		final var opBuff = threadLocalOpBuff.get();
		var pendingOpCount = opBuff.size();
		var n = batchSize - pendingOpCount;

		try {

			if (n > 0) { // the tasks buffer has free space for the new tasks
				if (itemInputFinishFlag) { // items input was exhausted
					if (!recycleFlag) { // never recycled -> recycling is not enabled
						opInputFinishFlag = true; // allow shutdown
					} else { // recycle the tasks if any
						n = recycleQueue.drainTo(opBuff, n);
						if (n > 0) {
							pendingOpCount += n;
							recycledOpCounter.add(n);
						}
					}
				} else {
					// try to produce new items from the items input
					if (inputLock.tryLock()) {
						try {
							// find the remaining count of the ops to generate
							final var remainingOpCount = countLimit - generatedOpCount();
							if (remainingOpCount > 0) {
								// make the limit not more than batch size
								n = (int) Math.min(remainingOpCount, n);
								final var items = getItems(itemInput, n);
								if (items == null) {
									itemInputFinishFlag = true;
									Loggers.MSG.debug(
													"End of items input \"{}\", generated op count: {}",
													itemInput.toString(),
													generatedOpCount());
								} else {
									n = items.size();
									if (n > 0) {
										pendingOpCount += buildOps(items, opBuff, n);
									}
								}
							}
						} finally {
							inputLock.unlock();
						}
					}
				}
			}

			if (outputOpCounter.sum() < countLimit) {

				if (pendingOpCount > 0) {

					n = pendingOpCount;

					// acquire the permit for all the throttles
					for (var i = 0; i < throttles.length; i++) {
						final var throttle = throttles[i];
						if (throttle instanceof Throttle) {
							n = ((Throttle) throttle).tryAcquire(n);
						} else if (throttle instanceof IndexThrottle) {
							n = ((IndexThrottle) throttle).tryAcquire(originIndex, n);
						} else {
							throw new AssertionError("Unexpected throttle type: " + throttle.getClass());
						}
					}

					// try to output
					if (n > 0) {
						if (n == 1) { // single mode branch
							try {
								final var op = opBuff.get(0);
								if (opOutput.put(op)) {
									outputOpCounter.increment();
									if (pendingOpCount == 1) {
										opBuff.clear();
									} else {
										opBuff.remove(0);
									}
								}
							} catch (final Exception e) {
								throwUncheckedIfInterrupted(e);
								if (e instanceof EOFException) {
									Loggers.MSG.debug("{}: finish due to output's EOF, {}", name, e);
									outputFinishFlag = true;
								} else {
									LogUtil.exception(Level.ERROR, e, "{}: operation output failure", name);
								}
							}
						} else { // batch mode branch
							try {
								n = opOutput.put(opBuff, 0, n);
								outputOpCounter.add(n);
								if (n < pendingOpCount) {
									opBuff.removeFirst(n);
								} else {
									opBuff.clear();
								}
							} catch (final Exception e) {
								throwUncheckedIfInterrupted(e);
								if (e instanceof EOFException) {
									Loggers.MSG.debug("{}: finish due to output's EOF, {}", name, e);
									outputFinishFlag = true;
								} else {
									LogUtil.trace(Loggers.ERR, Level.ERROR, e, "Unexpected failure");
								}
							}
						}
					}
				}
			} else { // operations count limit is reached
				outputFinishFlag = true;
			}

		} catch (final EOFException ok) {} catch (final Throwable t) {
			throwUncheckedIfInterrupted(t);
			LogUtil.trace(Loggers.ERR, Level.ERROR, t, "{}: unexpected failure", name);
		} finally {
			if (isFinished()) {
				try {
					stop();
				} catch (final IllegalStateException ignored) {}
			}
		}
	}

	private static <I extends Item> List<I> getItems(final Input<I> itemInput, final int n) {
		final List<I> items = new ArrayList<>(n); // prepare the items buffer
		try {
			itemInput.get(items, n); // get the items from the input
		} catch (final Exception e) {
			throwUncheckedIfInterrupted(e);
			if (e instanceof EOFException) {
				return null;
			}
		}
		return items;
	}

	// build new tasks for the corresponding items
	private long buildOps(final List<I> items, final CircularBuffer<O> opBuff, final int n)
					throws IOException {
		if (shuffleFlag) {
			Collections.shuffle(items, rnd);
		}
		try {
			opsBuilder.buildOps(items, opBuff);
			builtTasksCounter.add(n);
			return n;
		} catch (final IllegalArgumentException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to generate the load operation");
		}
		return 0;
	}

	@Override
	public final boolean isItemInputFinished() {
		return itemInputFinishFlag;
	}

	@Override
	public final long generatedOpCount() {
		return builtTasksCounter.sum() + recycledOpCounter.sum();
	}

	@Override
	public final void recycle(final O op) {
		if (!recycleQueue.offer(op)) {
			if (!recycleQueueFullState && 0 == recycleQueue.remainingCapacity()) {
				recycleQueueFullState = true;
				Loggers.ERR.error("{}: cannot recycle the operation, queue is full", name);
			}
		}
	}

	@Override
	public final boolean isNothingToRecycle() {
		return recycleQueue.isEmpty();
	}

	private boolean isFinished() {
		return outputFinishFlag
						|| itemInputFinishFlag && opInputFinishFlag && generatedOpCount() == outputOpCounter.sum();
	}

	@Override
	protected final void doStop() throws IllegalStateException {
		super.doStop();
		Loggers.MSG.debug(
						"{}: generated {}, recycled {}, output {} operations",
						LoadGeneratorImpl.this.toString(),
						builtTasksCounter.sum(),
						recycledOpCounter.sum(),
						outputOpCounter.sum());
	}

	@Override
	protected final void doClose() {
		recycleQueue.clear();
		// the item input may be instantiated by the load generator builder which has no reference to it
		// so the load
		// generator builder should close it
		if (itemInput != null) {
			try {
				inputLock.tryLock(Fiber.WARN_DURATION_LIMIT_NANOS, TimeUnit.NANOSECONDS);
				itemInput.close();
			} catch (final InterruptedException e) {
				throwUnchecked(e);
			} catch (final Exception e) {
				LogUtil.exception(Level.WARN, e, "{}: failed to close the item input", toString());
			}
		}
		// ops builder is instantiated by the load generator builder which forgets it so the load
		// generator should close it
		opsBuilder.close();
	}

	@Override
	public final String toString() {
		return name;
	}
}
