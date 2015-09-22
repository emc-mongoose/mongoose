package com.emc.mongoose.core.impl.load.model;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.AsyncDataItemDst;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 26.05.15.
 */
public abstract class AsyncDataItemDstBase<T extends DataItem>
extends Thread
implements AsyncDataItemDst<T> {
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
	public AsyncDataItemDstBase(
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
	//
	protected void putActually(final T item)
	throws InterruptedException {
		if(item == null || counterPreSubm.get() >= maxCount) {
			shutdown();
		}
		if(isShutdown.get()) {
			throw new InterruptedException("Shut down already");
		}
		queue.put(item);
		counterPreSubm.incrementAndGet();
	}
	/**
	 May block the executing thread until the queue becomes able to ingest more
	 @param item
	 @throws RemoteException
	 @throws InterruptedException
	 @throws RejectedExecutionException
	 */
	@Override
	public void put(final T item)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		if(isStarted.get()) {
			putActually(item);
		} else {
			throw new RejectedExecutionException("Consuming is not started yet");
		}
	}
	//
	@Override
	public int put(final List<T> items, final int from, final int to)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		if(isShutdown.get()) {
			throw new InterruptedException("Shut down already");
		}
		int i = from;
		if(isStarted.get()) {
			while(i < to) {
				putActually(items.get(i));
				i++;
			}
		} else {
			throw new RejectedExecutionException("Consuming is not started yet");
		}
		return i - from;
	}
	/** Consumes the queue */
	@Override
	public void run() {
		LOG.debug(
			Markers.MSG, "Determined put queue capacity of {} for \"{}\"",
			queue.remainingCapacity(), getName()
		);
		long i = 0;
		int n, m;
		try {
			while(i < maxCount && !isInterrupted()) {
				n = queue.drainTo(buff, batchSize);
				if(n > 0) {
					if(shuffle) {
						Collections.shuffle(buff);
					}
					m = 0;
					while(m < n) {
						m += feedSeqBatch(buff, m, n);
					}
					i += m;
				} else {
					if(isShutdown.get()) {
						LOG.debug(
							Markers.MSG,
							"No items are available for consuming and shutdown flag is set"
						);
						break;
					} else {
						LockSupport.parkNanos(1);
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
	@Deprecated
	protected abstract void feedSeq(final T item)
	throws InterruptedException, RemoteException;
	//
	protected abstract int feedSeqBatch(final List<T> items, final int from, final int to)
	throws InterruptedException, RemoteException;
	//
	@Override
	public void shutdown() {
		if(!isStarted.get()) {
			throw new IllegalStateException(
				getName() + ": not started yet, but shutdown is invoked"
			);
		} else if(isShutdown.compareAndSet(false, true)) {
			LOG.debug(Markers.MSG, "{}: consumed {} items", getName(), counterPreSubm.get());
		} else {
			throw new IllegalStateException(getName() + ": shut down already");
		}
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
				Markers.MSG, "{}: dropped {} put tasks", getClass().getSimpleName(), dropCount
			);
		}
		queue.clear(); // dispose
		if(!super.isInterrupted()) {
			super.interrupt();
		}
	}
}
