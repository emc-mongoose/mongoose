package com.emc.mongoose.storage.driver.nio.base;

import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
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
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 19.07.16.
 The multi-threaded non-blocking I/O storage driver.
 */
public abstract class NioStorageDriverBase<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends StorageDriverBase<I, O, R>
implements StorageDriver<I, O, R> {

	private final static Logger LOG = LogManager.getLogger();
	private final static int MIN_TASK_BUFF_CAPACITY = 0x4000;

	private final ThreadPoolExecutor ioTaskExecutor;
	private final int ioWorkerCount;
	private final int ioTaskBuffCapacity;
	private final Runnable ioWorkerTasks[];
	private final BlockingQueue<O> ioTaskQueues[];

	@SuppressWarnings("unchecked")
	public NioStorageDriverBase(
		final String jobName, final LoadConfig loadConfig, final boolean verifyFlag
	) {
		super(jobName, null, loadConfig, verifyFlag);
		ioWorkerCount = Math.min(concurrencyLevel, ThreadUtil.getHardwareConcurrencyLevel());
		ioWorkerTasks = new Runnable[ioWorkerCount];
		ioTaskQueues = new BlockingQueue[ioWorkerCount];
		ioTaskBuffCapacity = Math.max(MIN_TASK_BUFF_CAPACITY, concurrencyLevel / ioWorkerCount);
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


	/**
	 The class represents the non-blocking I/O task execution algorithm.
	 The I/O task itself may correspond to a large data transfer so it can't be non-blocking.
	 So the I/O task may be invoked multiple times (in the reentrant manner).
	 */
	private final class NioWorkerTask
	implements Runnable {
		
		@SuppressWarnings("unchecked")
		private final List<O> ioTaskBuff = new ArrayList<>(ioTaskBuffCapacity);
		private final BlockingQueue<O> ioTaskQueue;

		public NioWorkerTask(final BlockingQueue<O> ioTaskQueue) {
			this.ioTaskQueue = ioTaskQueue;
		}

		@Override
		public final void run() {

			Iterator<O> ioTaskIterator;
			int ioTaskBuffSize;
			O ioTask;

			while(!isInterrupted() && !isClosed()) {

				ioTaskBuffSize = ioTaskBuff.size();
				// get the new I/O tasks from the common queue
				// if there's a free place for the new I/O tasks
				if(ioTaskBuffSize < ioTaskBuffCapacity) {
					ioTaskBuffSize += ioTaskQueue.drainTo(
						ioTaskBuff, ioTaskBuffCapacity - ioTaskBuffSize
					);
				}

				if(ioTaskBuffSize > 0) {
					ioTaskIterator = ioTaskBuff.iterator();
					while(ioTaskIterator.hasNext()) {
						ioTask = ioTaskIterator.next();
						// check if the task is invoked 1st time
						if(IoTask.Status.PENDING.equals(ioTask.getStatus())) {
							// respect the configured concurrency level
							if(!concurrencyThrottle.tryAcquire()) {
								// do not start the new task if there max count of active tasks
								// reached
								continue;
							}
							// mark the task as active
							ioTask.startRequest();
							ioTask.finishRequest();
						}
						// perform non blocking I/O for the task
						invokeNio(ioTask);
						// remove the task from the buffer if it is not active more
						if(!IoTask.Status.ACTIVE.equals(ioTask.getStatus())) {
							concurrencyThrottle.release();
							ioTaskIterator.remove();
							ioTaskCompleted(ioTask);
						} // else the task remains in the buffer for the next iteration
					}
				} else {
					LockSupport.parkNanos(1);
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
	protected final void submit(final O ioTask)
	throws InterruptedIOException {
		final BlockingQueue<O> nextQueue = ioTaskQueues[
			Math.abs(ioTask.hashCode()) % ioWorkerCount
		];
		ioTask.reset();
		if(nextQueue != null) {
			try {
				nextQueue.put(ioTask);
			} catch(final InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
	}

	@Override
	protected final int submit(final List<O> ioTasks, final int from, final int to)
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
	protected final int submit(final List<O> ioTasks)
	throws InterruptedIOException {
		return submit(ioTasks, 0, ioTasks.size());
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
