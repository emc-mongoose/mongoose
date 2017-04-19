package com.emc.mongoose.load.generator;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.common.collection.OptLockBuffer;
import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.common.concurrent.WeightThrottle;
import com.emc.mongoose.model.DaemonBase;
import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadGenerator;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 Created by kurila on 11.07.16.
 */
public class BasicLoadGenerator<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadGenerator<I, O>, SvcTask {

	private static final Logger LOG = LogManager.getLogger();

	private volatile WeightThrottle weightThrottle = null;
	private volatile Throttle<Object> rateThrottle = null;
	private volatile Output<O> ioTaskOutput;
	private volatile boolean finishAllowedFlag = true;

	private final Input<I> itemInput;
	private final Lock inputLock = new ReentrantLock();
	private final SizeInBytes itemSizeEstimate;
	private final Random rnd;
	private final long countLimit;
	private final boolean shuffleFlag;
	private final IoTaskBuilder<I, O> ioTaskBuilder;
	private final int originCode;
	private final OptLockBuffer<O> remainingTasks = new OptLockArrayBuffer<>(BATCH_SIZE);

	private final LongAdder generatedTaskCounter = new LongAdder();
	private final String name;

	@SuppressWarnings("unchecked")
	public BasicLoadGenerator(
		final Input<I> itemInput, final SizeInBytes itemSizeEstimate,
		final IoTaskBuilder<I, O> ioTaskBuilder, final long countLimit, final SizeInBytes sizeLimit,
		final boolean shuffleFlag
	) throws UserShootHisFootException {

		this.itemInput = itemInput;
		this.itemSizeEstimate = itemSizeEstimate;
		this.ioTaskBuilder = ioTaskBuilder;
		this.originCode = ioTaskBuilder.hashCode();
		if(countLimit > 0) {
			this.countLimit = countLimit;
		} else if(
			sizeLimit.get() > 0 && itemSizeEstimate.get() > 0 &&
				itemSizeEstimate.getMin() == itemSizeEstimate.getMax()
		) {
			this.countLimit = sizeLimit.get() / itemSizeEstimate.get();
		} else {
			this.countLimit = Long.MAX_VALUE;
		}
		this.shuffleFlag = shuffleFlag;
		this.rnd = shuffleFlag ? new Random() : null;

		final String ioStr = ioTaskBuilder.getIoType().toString();
		name = Character.toUpperCase(ioStr.charAt(0)) + ioStr.substring(1).toLowerCase() +
			(countLimit > 0 && countLimit < Long.MAX_VALUE ? Long.toString(countLimit) : "") +
			itemInput.toString();
		svcTasks.add(this);
	}
	
	@Override
	public final void setWeightThrottle(final WeightThrottle weightThrottle) {
		this.weightThrottle = weightThrottle;
	}

	@Override
	public final void setRateThrottle(final Throttle<Object> rateThrottle) {
		this.rateThrottle = rateThrottle;
	}

	@Override
	public final void setOutput(final Output<O> ioTaskOutput) {
		this.ioTaskOutput = ioTaskOutput;
	}

	@Override
	public final long getGeneratedIoTasksCount() {
		return generatedTaskCounter.sum();
	}

	@Override
	public final SizeInBytes getItemSizeEstimate() {
		return itemSizeEstimate;
	}

	@Override
	public final IoType getIoType() {
		return ioTaskBuilder.getIoType();
	}
	
	private static final ThreadLocal<List> THREAD_LOCAL_BUFF = ThreadLocal.withInitial(
		(Supplier<List>) () -> new ArrayList(BATCH_SIZE)
	);
	
	@Override
	public final void run() {
		
		final List<O> thrLocBuff;
		if(!remainingTasks.tryLock()) {
			return;
		} else {
			thrLocBuff = THREAD_LOCAL_BUFF.get();
			thrLocBuff.clear();
			thrLocBuff.addAll(remainingTasks);
			remainingTasks.clear();
			remainingTasks.unlock();
		}
		int n = thrLocBuff.size();
		int m = BATCH_SIZE - n;
		boolean finishFlag = false;

		try {
			if(m > 0) {
				if(inputLock.tryLock()) {
					try {
						// find the limits and prepare the items buffer
						final long remainingTasksCount = countLimit - generatedTaskCounter.sum();
						if(remainingTasksCount > 0) {
							m = (int) Math.min(remainingTasksCount, m);
							final List<I> items = new ArrayList<>(m);
							try {
								// get the items from the input
								finishAllowedFlag = false;
								itemInput.get(items, m);
							} catch(final EOFException e) {
								LOG.debug(
									Markers.MSG, "{}: end of items input @ the count {}",
									BasicLoadGenerator.this.toString(), generatedTaskCounter.sum()
								);
								finishFlag = true;
							}
							
							m = items.size();
							if(m > 0) {
								if(shuffleFlag) {
									Collections.shuffle(items, rnd);
								}
								ioTaskBuilder.getInstances(items, thrLocBuff);
								generatedTaskCounter.add(m);
								n += m;
							}
						}
					} finally {
						inputLock.unlock();
					}
				}
				LockSupport.parkNanos(1);
			}

			if(n > 0) {
				// acquire the throttles permit
				m = n;
				if(weightThrottle != null) {
					m = weightThrottle.tryAcquire(originCode, m);
				}
				if(rateThrottle != null) {
					m = rateThrottle.tryAcquire(originCode, m);
				}
				// try to output
				if(m > 0) {
					try {
						m = ioTaskOutput.put(thrLocBuff, 0, m);
						if(m < n) {
							LockSupport.parkNanos(1);
							remainingTasks.lock();
							try {
								remainingTasks.addAll(thrLocBuff.subList(m, n));
							} finally {
								remainingTasks.unlock();
							}
						}
						finishAllowedFlag = true;
					} catch(final EOFException e) {
						LOG.debug(
							Markers.MSG, "{}: finish due to output's EOF",
							BasicLoadGenerator.this.toString()
						);
						finishFlag = true;
					} catch(final RemoteException e) {
						final Throwable cause = e.getCause();
						if(cause instanceof EOFException) {
							LOG.debug(
								Markers.MSG, "{}: finish due to output's EOF",
								BasicLoadGenerator.this.toString()
							);
							finishFlag = true;
						} else {
							LogUtil.exception(LOG, Level.ERROR, cause, "Unexpected failure");
							e.printStackTrace(System.err);
						}
					}
				}
			} else {
				finishAllowedFlag = true;
			}

		} catch(final Throwable t) {
			if(!(t instanceof EOFException)) {
				LogUtil.exception(LOG, Level.ERROR, t, "Unexpected failure");
				t.printStackTrace(System.err);
			}
		} finally {
			if(finishFlag && finishAllowedFlag) {
				LOG.debug(
					Markers.MSG, "{}: produced {} items", BasicLoadGenerator.this.toString(),
					generatedTaskCounter
				);
				try {
					shutdown();
				} catch(final IllegalStateException ignored) {
				}
			}
		}
	}

	@Override
	protected final void doShutdown() {
		interrupt();
	}

	@Override
	protected final void doInterrupt() {
		svcTasks.remove(this);
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		long remainingMillis = timeUnit.toMillis(timeout);
		long t;
		while(remainingMillis > 0) {
			t = System.currentTimeMillis();
			synchronized(state) {
				state.wait(remainingMillis);
			}
			if(!isStarted()) {
				return true;
			} else {
				t = System.currentTimeMillis() - t;
				remainingMillis -= t;
			}
		}
		return false;
	}

	@Override
	protected final void doClose()
	throws IOException {
		super.doClose();
		if(itemInput != null) {
			itemInput.close();
		}
		ioTaskBuilder.close();
		remainingTasks.clear();
		ioTaskOutput.close();
	}
	
	@Override
	public final String toString() {
		return name;
	}
	
	@Override
	public final int hashCode() {
		return originCode;
	}
}
