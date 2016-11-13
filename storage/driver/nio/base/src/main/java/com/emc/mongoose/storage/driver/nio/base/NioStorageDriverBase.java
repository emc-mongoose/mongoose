package com.emc.mongoose.storage.driver.nio.base;

import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import com.emc.mongoose.ui.log.Markers;

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

	private static final Logger LOG = LogManager.getLogger();

	private final ThreadPoolExecutor ioTaskExecutor;
	private final int ioWorkerCount;
	private final int ioTaskBuffCapacity;
	private final WorkerTask ioWorkerTasks[];
	private final BlockingQueue<O> ioTaskQueues[];

	@SuppressWarnings("unchecked")
	public NioStorageDriverBase(
		final String jobName, final LoadConfig loadConfig, final boolean verifyFlag
	) {
		super(jobName, null, loadConfig, verifyFlag);
		ioWorkerCount = ThreadUtil.getHardwareConcurrencyLevel();
		ioWorkerTasks = new WorkerTask[ioWorkerCount];
		ioTaskQueues = new BlockingQueue[ioWorkerCount];
		ioTaskBuffCapacity = Math.max(
			1, concurrencyLevel / ThreadUtil.getHardwareConcurrencyLevel()
		);
		for(int i = 0; i < ioWorkerCount; i ++) {
			ioTaskQueues[i] = new ArrayBlockingQueue<>(ioTaskBuffCapacity);
			ioWorkerTasks[i] = new NioWorkerTask(ioTaskQueues[i]);
		}
		ioTaskExecutor = new ThreadPoolExecutor(
			ioWorkerCount, ioWorkerCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(ioWorkerCount),
			new NamingThreadFactory(this.jobName + "-ioWorker", true)
		);
	}


	private interface WorkerTask
	extends Runnable {

		boolean isIdle();
	}

	/**
	 The class represents the non-blocking I/O task execution algorithm.
	 The I/O task itself may correspond to a large data transfer so it can't be non-blocking.
	 So the I/O task may be invoked multiple times (in the reentrant manner).
	 */
	private final class NioWorkerTask
	implements WorkerTask {
		
		@SuppressWarnings("unchecked")
		private final List<O> ioTaskBuff = new ArrayList<>(ioTaskBuffCapacity);
		private final BlockingQueue<O> ioTaskQueue;

		public NioWorkerTask(final BlockingQueue<O> ioTaskQueue) {
			this.ioTaskQueue = ioTaskQueue;
		}

		private volatile boolean idleFlag = false;

		@Override
		public final boolean isIdle() {
			return idleFlag;
		}

		@Override
		public final void run() {

			Iterator<O> ioTaskIterator;
			int ioTaskBuffSize;
			O ioTask;

			while(!isInterrupted() && !isClosed()) {

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
							ioTaskCompleted(ioTask);
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
		for(final Runnable ioWorkerTask : ioWorkerTasks) {
			ioTaskExecutor.execute(ioWorkerTask);
		}
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
		for(final WorkerTask ioWorkerTask : ioWorkerTasks) {
			if(isClosed() || isInterrupted()) {
				return true;
			}
			if(!ioWorkerTask.isIdle()) {
				return false;
			}
		}
		return true; // I/O task queue is empty and all I/O workers are idle
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
		final BlockingQueue<O> nextQueue = ioTaskQueues[
			Math.abs(ioTask.hashCode()) % ioWorkerCount
		];
		if(nextQueue != null) {
			try {
				nextQueue.put(ioTask);
			} catch(final InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
	}

	@Override
	public int put(final List<O> ioTasks, final int from, final int to)
	throws InterruptedIOException {
		try {
			O nextIoTask;
			BlockingQueue<O> nextQueue;
			for(int i = from; i < to; i ++) {
				nextIoTask = ioTasks.get(i);
				nextIoTask.reset();
				nextQueue = ioTaskQueues[Math.abs(nextIoTask.hashCode()) % ioWorkerCount];
				if(nextQueue != null) {
					nextQueue.put(nextIoTask);
				}
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
		for(int i = 0; i < ioWorkerCount; i ++) {
			ioWorkerTasks[i] = null;
			ioTaskQueues[i].clear();
			ioTaskQueues[i] = null;
		}
	}
}
