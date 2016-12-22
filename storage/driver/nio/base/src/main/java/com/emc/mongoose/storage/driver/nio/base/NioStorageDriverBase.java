package com.emc.mongoose.storage.driver.nio.base;

import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
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

			while(isStarted() || isShutdown()) {

				ioTaskBuffSize = ioTaskBuff.size();
				// get the new I/O tasks from the common queue if there's a free place for the new
				// I/O tasks and the state is active (not yet shutdown)
				if(isStarted() && ioTaskBuffSize < ioTaskBuffCapacity) {
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
							// do not start the new task if the state is not more active
							if(!isStarted()) {
								ioTaskIterator.remove();
								continue;
							}
							// respect the configured concurrency level
							if(!concurrencyThrottle.tryAcquire()) {
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

			ioTaskBuffSize = ioTaskBuff.size();
			LOG.debug(Markers.MSG, "Finish {} remaining active tasks finally", ioTaskBuffSize);
			for(int i = 0; i < ioTaskBuffSize; i ++) {
				ioTask = ioTaskBuff.get(i);
				while(IoTask.Status.ACTIVE.equals(ioTask.getStatus())) {
					invokeNio(ioTask);
				}
				concurrencyThrottle.release();
				ioTaskCompleted(ioTask);
			}
			LOG.debug(Markers.MSG, "Finish the remaining active tasks done");
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
	protected final void doStart()
	throws IllegalStateException {
		for(final Runnable ioWorkerTask : ioWorkerTasks) {
			ioTaskExecutor.execute(ioWorkerTask);
		}
	}

	@Override
	protected final void doShutdown()
	throws IllegalStateException {
		ioTaskExecutor.shutdown();
	}

	@Override
	protected final void doInterrupt()
	throws IllegalStateException {
		try {
			if(!ioTaskExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				LOG.error(Markers.ERR, "Failed to stop the remaining I/O tasks in 1 second");
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Unexpected interruption");
		}
		ioTaskExecutor.shutdownNow();
		assert ioTaskExecutor.isTerminated();
		super.doInterrupt();
	}

	@Override
	protected final boolean submit(final O ioTask)
	throws InterruptedException {
		ioTask.reset();
		for(int i = 0; i < ioWorkerCount; i ++) {
			if(!isStarted()) {
				throw new InterruptedException();
			}
			if(ioTaskQueues[(int) (System.nanoTime() % ioWorkerCount)].offer(ioTask)) {
				return true;
			} else {
				i ++;
			}
		}
		return false;
	}

	@Override
	protected final int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException {
		O nextIoTask;
		int i = from, j;
		while(i < to) {
			nextIoTask = ioTasks.get(i);
			nextIoTask.reset();
			for(j = 0; j < ioWorkerCount; j ++) {
				if(!isStarted()) {
					throw new InterruptedException();
				}
				if(ioTaskQueues[(int) (System.nanoTime() % ioWorkerCount)].offer(nextIoTask)) {
					i ++;
					break;
				}
			}
		}
		return i - from;
	}
	
	@Override
	protected final int submit(final List<O> ioTasks)
	throws InterruptedException {
		return submit(ioTasks, 0, ioTasks.size());
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
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
