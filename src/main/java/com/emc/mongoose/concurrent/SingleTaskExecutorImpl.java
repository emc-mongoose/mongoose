package com.emc.mongoose.concurrent;

import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import org.apache.logging.log4j.Level;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public final class SingleTaskExecutorImpl
implements Runnable, SingleTaskExecutor {

	private static final ThreadFactory THREAD_FACTORY = new LogContextThreadFactory("single_task_executor_", true);

	private final AtomicReference<Thread> workerRef = new AtomicReference<>(null);
	private final AtomicReference<Runnable> taskRef = new AtomicReference<>(null);

	public SingleTaskExecutorImpl() {
		startWorker();
	}

	private void startWorker() {
		if(workerRef.compareAndSet(null, THREAD_FACTORY.newThread(this))) {
			workerRef.get().start();
		}
	}

	private void stopWorker() {
		final Thread worker = workerRef.get();
		if(null != worker) {
			worker.interrupt();
		}
	}

	@Override
	public final void run() {
		Runnable task;
		while(true) {
			task = taskRef.get();
			if(null != task) {
				try {
					task.run();
				} catch(final InterruptRunException e) {
					throw e;
				} catch(final Throwable cause) {
					LogUtil.trace(Loggers.ERR, Level.ERROR, cause, "Unexpected task execution failure");
				} finally {
					taskRef.set(null);
				}
			} else {
				LockSupport.parkNanos(1);
			}
		}
	}

	@Override
	public final void execute(final Runnable task)
	throws RejectedExecutionException {
		if(! taskRef.compareAndSet(null, task)) {
			throw new RejectedExecutionException();
		}
	}

	@Override
	public final Runnable task() {
		return taskRef.get();
	}

	@Override
	public final boolean stop(final Runnable task) {
		if(taskRef.compareAndSet(task, null)) {
			stopWorker();
			startWorker();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public final void close() {
		stopWorker();
		taskRef.set(null);
	}
}
