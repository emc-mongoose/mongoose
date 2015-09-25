package com.emc.mongoose.core.impl.load.model;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.DataItemConsumer;
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
public abstract class AsyncDataItemConsumerBase<T extends DataItem>
extends Thread
implements DataItemConsumer<T> {
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
		isInterrupted = new AtomicBoolean(false),
		isClosed = new AtomicBoolean(false);
	// volatile
	protected final BlockingQueue<T> queue;
	private final List<T> buff;
	private final boolean shuffle;
	private final int batchSize;
	//
	public AsyncDataItemConsumerBase(
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
	public final void start()
	throws IllegalStateException {
		if(isStarted.compareAndSet(false, true)) {
			startActually();
		} else {
			throw new IllegalStateException("Started already");
		}
	}
	//
	protected void startActually() {
		super.start();
		LOG.debug(
			Markers.MSG,
			"{}: started, the further consuming will go through the volatile queue",
			getName()
		);
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
				i ++;
			}
		} else {
			throw new RejectedExecutionException("Consuming is not started yet");
		}
		return i - from;
	}
	//
	@Override
	public final int put(final List<T> items)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		return put(items, 0, items.size());
	}
	//
	protected void putActually(final T item)
		throws InterruptedException {
		if(item == null || counterPreSubm.get() >= maxCount) {
			if(!isShutdown.compareAndSet(false, true)) {
				shutdownActually();
			}
		}
		if(isShutdown.get()) {
			throw new InterruptedException("Shut down already");
		}
		queue.put(item);
		counterPreSubm.incrementAndGet();
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
						m += feedSeq(buff, m, n);
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
				buff.clear();
			}
			LOG.debug(Markers.MSG, "{}: consuming finished @ count {}", getName(), i);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "{}: consuming interrupted @ count {}", getName(), i);
		} catch(final RejectedExecutionException e) {
			LOG.debug(Markers.MSG, "{}: consuming rejected @ count {}", getName(), i);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Submit item failure @ count {}", i);
		} finally {
			if(!isShutdown.compareAndSet(false, true)) {
				shutdownActually();
			}
		}
	}
	//
	protected abstract int feedSeq(final List<T> items, final int from, final int to)
	throws InterruptedException, RemoteException;
	//
	@Override
	public final void shutdown() {
		if(!isStarted.get()) {
			throw new IllegalStateException(
				getName() + ": not started yet, but shutdown is invoked"
			);
		} else if(isShutdown.compareAndSet(false, true)) {
			shutdownActually();
		} else {
			throw new IllegalStateException(getName() + ": shut down already");
		}
	}
	//
	protected void shutdownActually() {
		LOG.debug(Markers.MSG, "{}: consumed {} items", getName(), counterPreSubm.get());
	}
	//
	@Override
	public final void await()
	throws InterruptedException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public void await(final long timeValue, final TimeUnit timeUnit)
	throws InterruptedException {
		join();
		LOG.debug(
			Markers.MSG, getName() + ": waiting for the queue remaining content processing is done"
		);
	}
	//
	@Override
	public final void interrupt() {
		if(isInterrupted.compareAndSet(false, true)) {
			interruptActually();
		} else {
			throw new IllegalStateException(getName() + ": has been interrupted already");
		}
	}
	//
	protected void interruptActually() {
		if(!isShutdown.compareAndSet(false, true)) {
			shutdownActually();
		}
		super.interrupt();
	}
	//
	@Override
	public final void close()
	throws IOException, IllegalStateException {
		if(isClosed.compareAndSet(false, true)) {
			closeActually();
		} else {
			throw new IllegalStateException(getName() + ": has been closed already");
		}
	}
	//
	protected void closeActually()
	throws IOException {
		if(isShutdown.compareAndSet(false, true)) {
			shutdownActually();
		}
		final int dropCount = queue.size();
		if(dropCount > 0) {
			LOG.debug(
				Markers.MSG, "{}: dropped {} put tasks", getClass().getSimpleName(), dropCount
			);
		}
		queue.clear(); // dispose
		if(isInterrupted.compareAndSet(false, true)) {
			interruptActually();
		}
	}
}
