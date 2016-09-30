package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.StorageDriver;
import com.emc.mongoose.model.util.SizeInBytes;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 19.07.16.
 The multi-threaded non-blocking I/O storage driver.
 */
public abstract class NioStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements StorageDriver<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	private final ThreadPoolExecutor ioTaskExecutor;
	private final int ioWorkerCount;
	private final List<NioWorkerTask> ioWorkerTasks;
	private final BlockingQueue<O> ioTaskQueue;

	public NioStorageDriverBase(
		final String runId, final LoadConfig loadConfig,
		final String srcContainer, final AuthConfig authConfig,
		final boolean verifyFlag, final SizeInBytes ioBuffSize
	) {
		super(runId, loadConfig, srcContainer, authConfig, verifyFlag);
		final long ioBuffSizeMin = ioBuffSize.getMin();
		final long ioBuffSizeMax = ioBuffSize.getMax();
		if(ioBuffSizeMin < 1 || ioBuffSizeMin > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Invalid I/O buff size min: " + ioBuffSizeMin);
		}
		if(ioBuffSizeMax < ioBuffSizeMin || ioBuffSizeMax > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Invalid I/O buff size max: " + ioBuffSizeMax);
		}
		ioTaskQueue = new ArrayBlockingQueue<>(loadConfig.getQueueConfig().getSize());
		ioWorkerCount = ThreadUtil.getAvailableConcurrencyLevel();
		ioWorkerTasks = new ArrayList<>(ioWorkerCount);
		for(int i = 0; i < ioWorkerCount; i ++) {
			ioWorkerTasks.add(new NioWorkerTask());
		}
		ioTaskExecutor = new ThreadPoolExecutor(
			ioWorkerCount, ioWorkerCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(ioWorkerCount),
			new com.emc.mongoose.model.util.IoWorker.Factory(
				this.runId + "-ioWorker", (int) ioBuffSize.getMin(), (int) ioBuffSize.getMax()
			)
		);
	}

	/**
	 The class represents the non-blocking I/O task execution algorithm.
	 The I/O task itself may correspond to a large data transfer so it can't be non-blocking.
	 So the I/O task may be invoked multiple times (in the reentrant manner).
	 */
	private final class NioWorkerTask
	implements Runnable {

		private final int ioTaskBuffCapacity = Math.max(
			1, concurrencyLevel / ThreadUtil.getAvailableConcurrencyLevel()
		);
		@SuppressWarnings("unchecked")
		private final List<O> ioTaskBuff = new ArrayList<>(ioTaskBuffCapacity);

		private volatile boolean idleFlag = false;

		public final boolean isIdle() {
			return idleFlag;
		}

		@Override
		public final void run() {

			Iterator<O> ioTaskIterator;
			int ioTaskBuffSize;
			O ioTask;

			while(!NioStorageDriverBase.this.isInterrupted()) {

				ioTaskBuffSize = ioTaskBuff.size();
				if(ioTaskBuffSize < ioTaskBuffCapacity) {
					ioTaskBuffSize += ioTaskQueue.drainTo(
						ioTaskBuff, ioTaskBuffCapacity - ioTaskBuffSize
					);
				}

				if(ioTaskBuffSize > 0) {
					idleFlag = false;
					ioTaskIterator = ioTaskBuff.iterator();
					while(ioTaskIterator.hasNext()) {
						ioTask = ioTaskIterator.next();
						// perform non blocking I/O for the task
						invokeNio(ioTask);
						// remove the task from the buffer if it is not active more
						if(!IoTask.Status.ACTIVE.equals(ioTask.getStatus())) {
							ioTaskIterator.remove();
							try {
								ioTaskCompleted(ioTask);
							} catch(final IOException e) {
								LogUtil.exception(LOG, Level.WARN, e,
									"Failed to invoke the I/O task completion callback"
								);
							}
						}
					}
				} else {
					idleFlag = true;
				}
			}
		}
	}

	/**
	 Reentrant method which decorates the actual non-blocking create/read/etc I/O operation.
	 May change the task status or not change if the I/O operation is not completed during this
	 particular invocation
	 @param ioTask
	 */
	protected abstract void invokeNio(final O ioTask);
	
	@Override
	protected void doStart()
	throws IllegalStateException {
		ioWorkerTasks.forEach(ioTaskExecutor::submit);
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {
		ioTaskExecutor.shutdown();
	}

	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		final List<Runnable> interruptedTasks = ioTaskExecutor.shutdownNow();
		LOG.debug(Markers.MSG, "{} I/O tasks dropped", interruptedTasks.size());
	}

	@Override
	public final boolean isIdle() {
		if(ioTaskQueue.isEmpty()) {
			for(final NioWorkerTask ioWorkerTask : ioWorkerTasks) {
				if(!ioWorkerTask.isIdle()) {
					return false;
				}
			}
			return true; // I/O task queue is empty and all I/O workers are idle
		} else {
			return false;
		}
	}

	@Override
	public final boolean isFullThrottleEntered() {
		// TODO
		return false;
	}

	@Override
	public final boolean isFullThrottleExited() {
		// TODO
		return false;
	}

	@Override
	public void put(final O ioTask)
	throws InterruptedIOException {
		try {
			ioTaskQueue.put(ioTask);
		} catch(final InterruptedException e) {
			throw new InterruptedIOException();
		}
	}

	@Override
	public int put(final List<O> ioTasks, final int from, final int to)
	throws InterruptedIOException {
		try {
			for(int i = from; i < to; i ++) {
				ioTaskQueue.put(ioTasks.get(i));
			}
		} catch(final InterruptedException e) {
			throw new InterruptedIOException();
		}
		return to - from;
	}
	
	@Override
	public int put(final List<O> ioTasks)
	throws InterruptedIOException {
		return put(ioTasks, 0, ioTasks.size());
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return ioTaskExecutor.awaitTermination(timeout, timeUnit);
	}

	@Override
	protected void doClose()
	throws IOException {
		super.doClose();
		ioTaskQueue.clear();
	}
}
