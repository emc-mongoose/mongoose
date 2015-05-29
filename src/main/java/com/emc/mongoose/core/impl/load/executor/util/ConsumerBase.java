package com.emc.mongoose.core.impl.load.executor.util;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
//
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 26.05.15.
 */
public abstract class ConsumerBase<T extends DataItem>
extends Thread
implements Consumer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final BlockingQueue<T> submitQueue;
	private final long maxCount;
	private final AtomicLong counterPreSubm = new AtomicLong(0);
	//
	protected final int submTimeOutMilliSec, maxQueueSize;
	protected final AtomicBoolean
		isShutdown = new AtomicBoolean(false),
		isAllSubm = new AtomicBoolean(false);
	//
	protected ConsumerBase(final RunTimeConfig runTimeConfig, final long maxCount) {
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		maxQueueSize = (int) Math.min(
			this.maxCount, runTimeConfig.getRunRequestQueueSize()
		);
		submitQueue = new ArrayBlockingQueue<>(maxQueueSize);
		submTimeOutMilliSec = runTimeConfig.getRunSubmitTimeOutMilliSec();
	}
	/**
	 May block the executing thread until the queue becomes able to ingest more
	 @param dataItem
	 @throws RemoteException
	 @throws InterruptedException
	 @throws RejectedExecutionException
	 */
	@Override
	public void submit(final T dataItem)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		//
		if(dataItem == null || counterPreSubm.get() >= maxCount) {
			shutdown();
		}
		if(isShutdown.get()) {
			throw new RejectedExecutionException("Shut down already");
		}
		//
		if(submitQueue.offer(dataItem, submTimeOutMilliSec, TimeUnit.MILLISECONDS)) {
			counterPreSubm.incrementAndGet();
		} else {
			throw new RejectedExecutionException("Submit queue timeout");
		}
	}
	//
	@Override
	public final void run() {
		LOG.debug(
			LogUtil.MSG, "Determined submit queue capacity of {} for \"{}\"",
			submitQueue.remainingCapacity(), getName()
		);
		T nextDataItem;
		try {
			while(submitQueue.size() > 0 || !isShutdown.get()) {
				nextDataItem = submitQueue.poll(submTimeOutMilliSec, TimeUnit.MILLISECONDS);
				if(nextDataItem != null) {
					submitSync(nextDataItem);
				}
			}
			LOG.debug(LogUtil.MSG, "{}: consuming finished", getName());
		} catch(final InterruptedException e) {
			LOG.debug(LogUtil.MSG, "{}: consuming interrupted", getName());
		} catch(final RejectedExecutionException e) {
			LOG.debug(LogUtil.MSG, "{}: consuming rejected", getName());
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Submit data item failure");
		} finally {
			shutdown();
			isAllSubm.set(true);
		}
	}
	//
	protected abstract void submitSync(final T dataItem)
	throws InterruptedException, RemoteException;
	//
	@Override
	public void shutdown() {
		if(isShutdown.compareAndSet(false, true)) {
			LOG.debug(LogUtil.MSG, "{}: consumed {} data items", getName(), counterPreSubm.get());
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
		submitQueue.clear(); // dispose
		final int dropCount = submitQueue.size();
		if(dropCount > 0) {
			LOG.debug(LogUtil.MSG, "Dropped {} submit tasks", dropCount);
		}
	}
}
