package com.emc.mongoose.storage.driver.nio.base;

import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.model.io.task.IoTask.Status.ACTIVE;
import static com.emc.mongoose.api.model.io.task.IoTask.Status.INTERRUPTED;
import static com.emc.mongoose.api.model.io.task.IoTask.Status.PENDING;
import com.emc.mongoose.api.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.api.common.collection.OptLockBuffer;
import com.emc.mongoose.api.model.concurrent.Coroutine;
import com.emc.mongoose.api.model.concurrent.CoroutineBase;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.common.concurrent.ThreadUtil;
import com.emc.mongoose.api.model.concurrent.ThreadDump;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.base.StorageDriverBase;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 19.07.16.
 The multi-threaded non-blocking I/O storage driver.
 Note that this kind of storage driver uses the service coroutines facility to execute the I/O
 */
public abstract class NioStorageDriverBase<I extends Item, O extends IoTask<I>>
extends StorageDriverBase<I, O>
implements NioStorageDriver<I, O> {

	private final static String CLS_NAME = NioStorageDriverBase.class.getSimpleName();

	private final int ioWorkerCount;
	private final int ioTaskBuffCapacity;
	private final List<Coroutine> ioCoroutines;
	private final OptLockBuffer<O> ioTaskBuffs[];
	private final AtomicLong rrc = new AtomicLong(0);

	@SuppressWarnings("unchecked")
	public NioStorageDriverBase(
		final String jobName, final DataInput contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException {
		super(jobName, contentSrc, loadConfig, storageConfig, verifyFlag);
		final int confWorkerCount = storageConfig.getDriverConfig().getThreads();
		if(confWorkerCount > 0) {
			ioWorkerCount = confWorkerCount;
		} else {
			ioWorkerCount = Math.min(concurrencyLevel, ThreadUtil.getHardwareThreadCount());
		}
		ioCoroutines = new ArrayList<>(ioWorkerCount);
		ioTaskBuffs = new OptLockBuffer[ioWorkerCount];
		ioTaskBuffCapacity = Math.max(MIN_TASK_BUFF_CAPACITY, concurrencyLevel / ioWorkerCount);
		for(int i = 0; i < ioWorkerCount; i ++) {
			ioTaskBuffs[i] = new OptLockArrayBuffer<>(ioTaskBuffCapacity);
			ioCoroutines.add(new NioCoroutine(svcCoroutines, ioTaskBuffs[i]));
		}
	}

	/**
	 The class represents the non-blocking I/O task execution algorithm.
	 The I/O task itself may correspond to a large data transfer so it can't be non-blocking.
	 So the I/O task may be invoked multiple times (in the reentrant manner).
	 */
	private final class NioCoroutine
	extends CoroutineBase
	implements Coroutine {

		private final OptLockBuffer<O> ioTaskBuff;
		private final List<O> ioTaskLocalBuff;

		private int ioTaskBuffSize;
		private O ioTask;

		public NioCoroutine(
			final List<Coroutine> svcCoroutines, final OptLockBuffer<O> ioTaskBuff
		) {
			super(svcCoroutines);
			this.ioTaskBuff = ioTaskBuff;
			this.ioTaskLocalBuff = new ArrayList<>(ioTaskBuffCapacity);
		}

		@Override
		protected final void invokeTimed(final long startTimeNanos) {
			if(ioTaskBuff.tryLock()) {
				ioTaskBuffSize = ioTaskBuff.size();
				if(ioTaskBuffSize > 0) {
					try {
						for(int i = 0; i < ioTaskBuffSize; i ++) {
							ioTask = ioTaskBuff.get(i);
							// if timeout, put the task into the temporary buffer
							if(System.nanoTime() - startTimeNanos >= TIMEOUT_NANOS) {
								ioTaskLocalBuff.add(ioTask);
								continue;
							}
							// check if the task is invoked 1st time
							if(PENDING.equals(ioTask.getStatus())) {
								// do not start the new task if the state is not more active
								if(!isStarted()) {
									continue;
								}
								// respect the configured concurrency level
								if(!concurrencyThrottle.tryAcquire()) {
									ioTaskLocalBuff.add(ioTask);
									continue;
								}
								// mark the task as active
								ioTask.startRequest();
								ioTask.finishRequest();
							}
							// perform non blocking I/O for the task
							invokeNio(ioTask);
							// remove the task from the buffer if it is not active more
							if(!ACTIVE.equals(ioTask.getStatus())) {
								concurrencyThrottle.release();
								ioTaskCompleted(ioTask);
							} else {
								// the task remains in the buffer for the next iteration
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
				}
			}
		}

		@Override
		protected final void doClose() {
			try {
				if(ioTaskBuff.tryLock(TIMEOUT_NANOS, TimeUnit.NANOSECONDS)) {
					ioTaskBuffSize = ioTaskBuff.size();
					Loggers.MSG.debug("Finish {} remaining active tasks finally", ioTaskBuffSize);
					for(int i = 0; i < ioTaskBuffSize; i ++) {
						ioTask = ioTaskBuff.get(i);
						if(ACTIVE.equals(ioTask.getStatus())) {
							ioTask.setStatus(INTERRUPTED);
							concurrencyThrottle.release();
							ioTaskCompleted(ioTask);
						}
					}
					Loggers.MSG.debug("Finish the remaining active tasks done");
				} else {
					Loggers.ERR.debug(
						"Failed to obtain the I/O tasks buff lock in time, thread dump:\n{}",
						new ThreadDump().toString()
					);
				}
			} catch(final InterruptedException ignored) {
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
		svcCoroutines.addAll(ioCoroutines);
	}

	@Override
	protected final void doInterrupt()
	throws IllegalStateException {
		for(final Coroutine ioCoroutine : ioCoroutines) {
			try {
				ioCoroutine.close();
			} catch(final IOException e) {
				Loggers.ERR.warn("{}: failed to stop and close the I/O coroutine", stepId);
			}
		}
		super.doInterrupt();
	}

	@Override
	protected final boolean submit(final O ioTask)
	throws IllegalStateException {
		OptLockBuffer<O> ioTaskBuff;
		for(int i = 0; i < ioWorkerCount; i ++) {
			if(!isStarted()) {
				throw new IllegalStateException();
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
	throws IllegalStateException {
		OptLockBuffer<O> ioTaskBuff;
		int j = from, k, n;
		for(int i = 0; i < ioWorkerCount; i ++) {
			if(!isStarted()) {
				throw new IllegalStateException();
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
	throws IllegalStateException {
		return submit(ioTasks, 0, ioTasks.size());
	}
	
	protected final void finishIoTask(final O ioTask) {
		try {
			ioTask.startResponse();
			ioTask.finishResponse();
			ioTask.setStatus(IoTask.Status.SUCC);
		} catch(final IllegalStateException e) {
			LogUtil.exception(
				Level.WARN, e, "{}: finishing the I/O task which is in an invalid state",
				ioTask.toString()
			);
			ioTask.setStatus(IoTask.Status.FAIL_UNKNOWN);
		}
	}
	
	@Override
	protected void doClose()
	throws IOException {
		super.doClose();
		for(int i = 0; i < ioWorkerCount; i ++) {
			try(final Instance logCtx = CloseableThreadContext.put(KEY_CLASS_NAME, CLS_NAME)) {
				if(ioTaskBuffs[i].tryLock(Coroutine.TIMEOUT_NANOS, TimeUnit.NANOSECONDS)) {
					ioTaskBuffs[i].clear();
				} else if(ioTaskBuffs[i].size() > 0){
					Loggers.ERR.debug(
						"Failed to obtain the I/O tasks buff lock in time, thread dump:\n{}",
						new ThreadDump().toString()
					);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(
					Level.WARN, e, "Unexpected failure, I/O tasks buff remains uncleared"
				);
			}
			ioTaskBuffs[i] = null;
		}
		ioCoroutines.clear();
	}
}
