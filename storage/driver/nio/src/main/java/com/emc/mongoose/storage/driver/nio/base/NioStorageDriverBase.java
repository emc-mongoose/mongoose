package com.emc.mongoose.storage.driver.nio.base;

import static com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.common.collection.OptLockBuffer;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.model.NamingThreadFactory;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 19.07.16.
 The multi-threaded non-blocking I/O storage driver.
 */
public abstract class NioStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements NioStorageDriver<I, O> {

	private final ThreadPoolExecutor ioTaskExecutor;
	private final int ioWorkerCount;
	private final int ioTaskBuffCapacity;
	private final Runnable ioWorkerTasks[];
	private final OptLockBuffer<O> ioTaskBuffs[];
	private final AtomicLong rrc = new AtomicLong(0);

	@SuppressWarnings("unchecked")
	public NioStorageDriverBase(
		final String jobName, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final boolean verifyFlag
	) throws UserShootHisFootException {
		super(jobName, loadConfig, storageConfig, verifyFlag);
		final int confWorkerCount = storageConfig.getDriverConfig().getIoConfig().getWorkers();
		if(confWorkerCount < 1) {
			ioWorkerCount = Math.min(concurrencyLevel, ThreadUtil.getHardwareThreadCount());
		} else {
			ioWorkerCount = confWorkerCount;
		}
		ioWorkerTasks = new Runnable[ioWorkerCount];
		ioTaskBuffs = new OptLockBuffer[ioWorkerCount];
		ioTaskBuffCapacity = Math.max(MIN_TASK_BUFF_CAPACITY, concurrencyLevel / ioWorkerCount);
		for(int i = 0; i < ioWorkerCount; i ++) {
			ioTaskBuffs[i] = new OptLockArrayBuffer<>(ioTaskBuffCapacity);
			ioWorkerTasks[i] = new NioWorkerTask(ioTaskBuffs[i]);
		}
		ioTaskExecutor = new ThreadPoolExecutor(
			ioWorkerCount, ioWorkerCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(ioWorkerCount),
			new NamingThreadFactory(toString() + "/ioWorker", true)
		);
	}


	/**
	 The class represents the non-blocking I/O task execution algorithm.
	 The I/O task itself may correspond to a large data transfer so it can't be non-blocking.
	 So the I/O task may be invoked multiple times (in the reentrant manner).
	 */
	private final class NioWorkerTask
	implements Runnable {

		private final OptLockBuffer<O> ioTaskBuff;

		public NioWorkerTask(final OptLockBuffer<O> ioTaskBuff) {
			this.ioTaskBuff = ioTaskBuff;
		}

		@Override
		public final void run() {

			int ioTaskBuffSize;
			O ioTask;
			final List<O> ioTaskLocalBuff = new ArrayList<>(ioTaskBuffCapacity);

			while(isStarted() || isShutdown()) {
				if(ioTaskBuff.tryLock()) {
					ioTaskBuffSize = ioTaskBuff.size();
					if(ioTaskBuffSize > 0) {
						try {
							for(int i = 0; i < ioTaskBuffSize; i ++) {
								ioTask = ioTaskBuff.get(i);
								// check if the task is invoked 1st time
								if(IoTask.Status.PENDING.equals(ioTask.getStatus())) {
									// do not start the new task if the state is not more active
									if(!isStarted()) {
										continue;
									}
									// respect the configured concurrency level
									if(!concurrencyThrottle.tryAcquire()) {
										break;
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
									ioTaskCompleted(ioTask);
								} else { // the task remains in the buffer for the next iteration
									ioTaskLocalBuff.add(ioTask);
								}
							}
						} catch(final Throwable cause) {
							LogUtil.exception(Level.ERROR, cause, "I/O worker failure");
						} finally {
							// put the active tasks back into the buffer
							ioTaskBuff.clear();
							ioTaskBuffSize = ioTaskLocalBuff.size();
							if(ioTaskBuffSize > 0) {
								for(int i = 0; i < ioTaskBuffSize; i ++) {
									ioTaskBuff.add(ioTaskLocalBuff.get(i));
								}
								ioTaskLocalBuff.clear();
							}
							ioTaskBuff.unlock();
						}
					} else {
						ioTaskBuff.unlock();
						LockSupport.parkNanos(1);
					}
				}
			}

			if(ioTaskBuff.tryLock()) {
				ioTaskBuffSize = ioTaskBuff.size();
				Loggers.MSG.debug("Finish {} remaining active tasks finally", ioTaskBuffSize);
				for(int i = 0; i < ioTaskBuffSize; i ++) {
					ioTask = ioTaskBuff.get(i);
					while(IoTask.Status.ACTIVE.equals(ioTask.getStatus())) {
						invokeNio(ioTask);
					}
					concurrencyThrottle.release();
					ioTaskCompleted(ioTask);
				}
				Loggers.MSG.debug("Finish the remaining active tasks done");
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
	protected final void doStart()
	throws IllegalStateException {
		super.doStart();
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
			if(!ioTaskExecutor.awaitTermination(250, TimeUnit.MILLISECONDS)) {
				Loggers.ERR.error("Failed to stop the remaining I/O tasks in 0.25 second");
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "Unexpected interruption");
		}
		ioTaskExecutor.shutdownNow();
		if(!ioTaskExecutor.isTerminated()) {
			Loggers.ERR.warn("I/O tasks executor is not finished after the interruption");
		}
		super.doInterrupt();
	}

	@Override
	protected final boolean submit(final O ioTask)
	throws InterruptedException {
		ioTask.reset();
		OptLockBuffer<O> ioTaskBuff;
		for(int i = 0; i < ioWorkerCount; i ++) {
			if(!isStarted()) {
				throw new InterruptedException();
			}
			ioTaskBuff = ioTaskBuffs[(int) (rrc.getAndIncrement() % ioWorkerCount)];
			if(ioTaskBuff.tryLock()) {
				try {
					return ioTaskBuff.size() < ioTaskBuffCapacity && ioTaskBuff.add(ioTask);
				} finally {
					ioTaskBuff.unlock();
				}
			} else {
				i ++;
			}
		}
		return false;
	}

	@Override
	protected final int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException {
		OptLockBuffer<O> ioTaskBuff;
		int j = from, k, n;
		for(int i = 0; i < ioWorkerCount; i ++) {
			if(!isStarted()) {
				throw new InterruptedException();
			}
			ioTaskBuff = ioTaskBuffs[(int) (rrc.getAndIncrement() % ioWorkerCount)];
			if(ioTaskBuff.tryLock()) {
				try {
					n = Math.min(to - j, ioTaskBuffCapacity - ioTaskBuff.size());
					for(k = 0; k < n; k ++) {
						ioTaskBuff.add(ioTasks.get(j + k));
					}
					j += n;
				} finally {
					ioTaskBuff.unlock();
				}
			}
		}
		return j - from;
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
			try {
				if(ioTaskBuffs[i].tryLock(250, TimeUnit.MILLISECONDS)) {
					ioTaskBuffs[i].clear();
				} else {
					Loggers.ERR.debug(
						"Failed to obtain the lock, I/O tasks buff remains uncleared"
					);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(
					Level.WARN, e, "Unexpected failure, I/O tasks buff remains uncleared"
				);
			}
			ioTaskBuffs[i] = null;
		}
	}
}
