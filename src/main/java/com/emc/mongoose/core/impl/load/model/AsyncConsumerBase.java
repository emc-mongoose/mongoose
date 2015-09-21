package com.emc.mongoose.core.impl.load.model;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 26.05.15.
 */
public abstract class AsyncConsumerBase<T>
extends Thread
implements AsyncConsumer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	// configuration params
	protected final long maxCount;
	protected final int maxQueueSize;
	// states
	protected final AtomicLong counterPreSubm = new AtomicLong(0);
	protected final AtomicBoolean
		isStarted = new AtomicBoolean(false),
		isShutdown = new AtomicBoolean(false),
		isAllSubm = new AtomicBoolean(false);
	// volatile
	protected final BlockingQueue<T> queue;
	private final List<T> buff;
	private final boolean shuffle;
	private final int batchSize;
	//
	public AsyncConsumerBase(
		final long maxCount, final int maxQueueSize, final boolean shuffle, final int batchSize
	) throws IllegalArgumentException {
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		if(maxQueueSize > 0) {
			this.maxQueueSize = (int) Math.min(this.maxCount, maxQueueSize);
		} else {
			throw new IllegalArgumentException("Invalid max queue size: " + maxQueueSize);
		}
		queue = new ArrayBlockingQueue<>(maxQueueSize);
		buff = new ArrayList<>(batchSize);
		this.shuffle = shuffle;
		this.batchSize = batchSize;
	}
	//
	@Override
	public void start() {
		if(isStarted.compareAndSet(false, true)) {
			startActually();
		}
	}
	//
	protected void startActually() {
		LOG.debug(
			Markers.MSG,
			"{}: started, the further consuming will go through the volatile queue",
			getName()
		);
		super.start();
	}
	/**
	 May block the executing thread until the queue becomes able to ingest more
	 @param item
	 @throws RemoteException
	 @throws InterruptedException
	 @throws RejectedExecutionException
	 */
	@Override
	public void feed(final T item)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		if(isStarted.get()) {
			if(item == null || counterPreSubm.get() >= maxCount) {
				shutdown();
			}
			if(isShutdown.get()) {
				throw new InterruptedException("Shut down already");
			}
			queue.put(item);
			counterPreSubm.incrementAndGet();
		} else {
			throw new RejectedExecutionException("Consuming is not started yet");
		}
	}
	//
	@Override
	public void feedBatch(final List<T> items)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		if(isStarted.get()) {
			final long remaining = maxCount - counterPreSubm.get();
			if(remaining > 0) {
				if(remaining < items.size()) {
					feedBatch(items.subList(0, (int) remaining));
					shutdown();
				} else {
					for(final T item : items) {
						if(item == null || counterPreSubm.get() >= maxCount) {
							shutdown();
						}
						if(isShutdown.get()) {
							throw new InterruptedException("Shut down already");
						}
						queue.put(item);
						counterPreSubm.incrementAndGet();
					}
				}
			} else {
				shutdown();
				throw new InterruptedException(maxCount + " items have been pre-consumed");
			}
		} else {
			throw new RejectedExecutionException("Consuming is not started yet");
		}
	}
	/** Consumes the queue */
	@Override
	public void run() {
		LOG.debug(
			Markers.MSG, "Determined feed queue capacity of {} for \"{}\"",
			queue.remainingCapacity(), getName()
		);
		T nextItem;
		long i = 0;
		int availItemCount;
		try {
			while(i < maxCount && !isInterrupted()) {
				availItemCount = queue.size();
				if(availItemCount == 0) {
					if(isShutdown.get()) {
						LOG.debug(
							Markers.MSG,
							"No items are available for consuming and shutdown flag is set"
						);
						break;
					} else {
						LockSupport.parkNanos(1);
					}
				} else if(availItemCount > 1) {
					if(shuffle) {
						availItemCount = queue.drainTo(buff, batchSize);
						Collections.shuffle(buff);
					} else {
						availItemCount = queue.drainTo(buff);
					}
					for(int j = 0; j < availItemCount && i < maxCount; j ++) {
						feedSeq(buff.get(j));
						i ++;
					}
					// Do buff.clear() because hasn't to feed the old data item the second time
					buff.clear();
				} else {
					nextItem = queue.poll(POLL_TIMEOUT_MILLISEC, TimeUnit.MILLISECONDS);
					if(nextItem != null) {
						feedSeq(nextItem);
						i ++;
					}
				}
			}
			LOG.debug(Markers.MSG, "{}: consuming finished @ count {}", getName(), i);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "{}: consuming interrupted @ count {}", getName(), i);
		} catch(final RejectedExecutionException e) {
			LOG.debug(Markers.MSG, "{}: consuming rejected @ count {}", getName(), i);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Submit item failure @ count {}", i);
		} finally {
			isAllSubm.set(true);
			shutdown();
		}
	}
	//
	protected abstract void feedSeq(final T item)
	throws InterruptedException, RemoteException;
	//
	protected abstract void feedSeqBatch(final List<T> items)
	throws InterruptedException, RemoteException;
	//
	@Override
	public void shutdown() {
		/*if(!isStarted.get()) {
			throw new IllegalStateException(
				getName() + ": not started yet, but shutdown is invoked"
			);
		} else */if(isShutdown.compareAndSet(false, true)) {
			LOG.debug(Markers.MSG, "{}: consumed {} items", getName(), counterPreSubm.get());
		}
	}
	//
	@Override
	public long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public synchronized void interrupt() {
		shutdown();
		if(!super.isInterrupted()) {
			super.interrupt();
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		shutdown();
		final int dropCount = queue.size();
		if(dropCount > 0) {
			LOG.debug(
				Markers.MSG, "{}: dropped {} feed tasks", getClass().getSimpleName(), dropCount
			);
		}
		queue.clear(); // dispose
		if(!super.isInterrupted()) {
			super.interrupt();
		}
	}
}
