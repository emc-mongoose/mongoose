package com.emc.mongoose.load.generator;

import com.emc.mongoose.common.api.SizeInBytes;
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
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 11.07.16.
 */
public class BasicLoadGenerator<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadGenerator<I, O>, Runnable {

	private static final Logger LOG = LogManager.getLogger();

	private volatile WeightThrottle weightThrottle = null;
	private volatile Throttle<Object> rateThrottle = null;
	private volatile Output<O> ioTaskOutput;

	private final Input<I> itemInput;
	private final Lock itemInputLock = new ReentrantLock();
	private final SizeInBytes itemSizeEstimate;
	private final Random rnd;
	private final long countLimit;
	private final boolean shuffleFlag;
	private final IoTaskBuilder<I, O> ioTaskBuilder;
	private final List<O> remainingTasks = new ArrayList<>(BATCH_SIZE);
	private final Lock remainingTasksLock = new ReentrantLock();

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
		if(!svcTasks.offer(this)) {
			LOG.error(Markers.ERR, "{}: failed to register new service task", toString());
		}
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
	
	@Override
	public final void run() {
		
		final List<O> ioTasks;
		if(!remainingTasksLock.tryLock()) {
			return;
		} else {
			ioTasks = new ArrayList<>(remainingTasks);
			remainingTasks.clear();
		}
		int n = ioTasks.size();
		int m = BATCH_SIZE - n;
		
		try {
			if(m > 0) {
				// find the limits and prepare the items buffer
				final long remainingTasksCount = countLimit - generatedTaskCounter.sum();
				if(remainingTasksCount > 0) {
					m = (int) Math.min(remainingTasksCount, m);
					if(itemInputLock.tryLock()) {
						final List<I> items = new ArrayList<>(m);
						try {
							// get the items from the input
							itemInput.get(items, m);
						} catch(final EOFException e) {
							LOG.debug(
								Markers.MSG, "{}: end of items input @ the count {}",
								BasicLoadGenerator.this.toString(), generatedTaskCounter
							);
							finish();
						} finally {
							itemInputLock.unlock();
						}
						
						m = items.size();
						if(m > 0) {
							if(shuffleFlag) {
								Collections.shuffle(items, rnd);
							}
							ioTaskBuilder.getInstances(items, ioTasks);
							generatedTaskCounter.add(m);
							n += m;
						}
					}
				}
			}
			
			if(n > 0) {
				m = acquireThrottlesPermit(ioTasks, 0, n);
				if(m > 0) {
					try {
						m = ioTaskOutput.put(ioTasks, 0, m);
						if(m < n) {
							remainingTasksLock.lock();
							try {
								remainingTasks.addAll(ioTasks.subList(m, n));
							} finally {
								remainingTasksLock.unlock();
							}
						}
					} catch(final EOFException e) {
						LOG.debug(
							Markers.MSG, "{}: finish due to output's EOF",
							BasicLoadGenerator.this.toString()
						);
						finish();
					} catch(final RemoteException e) {
						final Throwable cause = e.getCause();
						if(cause instanceof EOFException) {
							LOG.debug(
								Markers.MSG, "{}: finish due to output's EOF",
								BasicLoadGenerator.this.toString()
							);
							finish();
						} else {
							LogUtil.exception(LOG, Level.ERROR, cause, "Unexpected failure");
							e.printStackTrace(System.err);
						}
					}
				}
			}
		} catch(final Throwable t) {
			LogUtil.exception(LOG, Level.ERROR, t, "Unexpected failure");
			t.printStackTrace(System.err);
			finish();
		}
	}
	
	/*@Override
	public final void run() {
		
		// find the limits and prepare the items buffer
		final long remaining = countLimit - generatedTaskCounter.sum();
		if(remaining <= 0) {
			finish();
		}
		int n = BATCH_SIZE > remaining ? (int) remaining : BATCH_SIZE;
		final List<I> items = new ArrayList<>(n);
		
		if(!itemInputLock.tryLock()) {
			return;
		} else {
			try {
				// get the items from the input
				n = itemInput.get(items, n);
			} catch(final EOFException e) {
				LOG.debug(
					Markers.MSG, "{}: end of items input @ the count {}",
					BasicLoadGenerator.this.toString(), generatedTaskCounter
				);
				finish();
			} catch(final Exception e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "{}: failed to get the items from the input",
					BasicLoadGenerator.this.toString()
				);
				finish();
			} finally {
				itemInputLock.unlock();
			}
		}
		
		if(n > 0) {
			if(shuffleFlag) {
				Collections.shuffle(items, rnd);
			}
			try {
				// build the I/O tasks for the items got from the input
				ioTaskBuilder.getInstances(items, ioTasks);
				int k = acquireThrottlesPermit(ioTasks, 0, n);
				if(k > 0) {
					k = ioTaskOutput.put(ioTasks, 0, k);
				}
				if(k < n) {
					synchronized(remainingTasks) {
						remainingTasks.addAll(ioTasks.subList(k, n));
					}
				}
				generatedTaskCounter.add(n);
			} catch(final EOFException e) {
				LOG.debug(
					Markers.MSG, "{}: finish due to output's EOF",
					BasicLoadGenerator.this.toString()
				);
				finish();
			} catch(final RemoteException e) {
				final Throwable cause = e.getCause();
				if(cause instanceof EOFException) {
					LOG.debug(
						Markers.MSG, "{}: finish due to output's EOF",
						BasicLoadGenerator.this.toString()
					);
					finish();
				} else if(!isStarted()){
					LogUtil.exception(LOG, Level.ERROR, cause, "Unexpected failure");
					e.printStackTrace(System.err);
				}
			} catch(final Exception e) {
				if(isStarted()) {
					LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
					e.printStackTrace(System.err);
				}
			}
		}
	}*/
	
	private void finish() {
		LOG.debug(
			Markers.MSG, "{}: produced {} items", BasicLoadGenerator.this.toString(),
			generatedTaskCounter
		);
		try {
			shutdown();
		} catch(final IllegalStateException ignored) {
		}
	}
	
	private int acquireThrottlesPermit(final List<O> ioTasks, final int from, final int to) {
		int n = to - from;
		final int originCode = hashCode();
		if(weightThrottle != null) {
			n = weightThrottle.tryAcquire(originCode, n);
		}
		if(rateThrottle != null) {
			n = rateThrottle.tryAcquire(originCode, n);
		}
		return n;
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
	}
	
	@Override
	public final String toString() {
		return name;
	}
	
	@Override
	public final int hashCode() {
		return ioTaskBuilder.hashCode();
	}
}
