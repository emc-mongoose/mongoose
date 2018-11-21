package com.emc.mongoose.concurrent;

import com.emc.mongoose.exception.InterruptRunException;
import com.emc.mongoose.logging.LogContextThreadFactory;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import org.apache.logging.log4j.Level;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

public final class SingleTaskExecutor
implements Runnable, Executor, Closeable {

	private static final ThreadFactory THREAD_FACTORY = new LogContextThreadFactory("single_task_executor_", true);

	private final Thread worker;
	private final AtomicReference<Runnable> runRef = new AtomicReference<>(null);

	public SingleTaskExecutor() {
		worker = THREAD_FACTORY.newThread(this);
		worker.start();
	}

	@Override
	public final void run() {
		Runnable task;
		while(true) {
			task = runRef.get();
			if(null != task) {
				try {
					task.run();
				} catch(final InterruptRunException e) {
					throw e;
				} catch(final Throwable cause) {
					LogUtil.trace(Loggers.ERR, Level.ERROR, cause, "Unexpected task execution failure");
				} finally {
					runRef.set(null);
				}
			}
		}
	}

	@Override
	public final void execute(final Runnable task)
	throws RejectedExecutionException {
		if(!runRef.compareAndSet(null, task)) {
			throw new RejectedExecutionException();
		}
	}

	@Override
	public final void close() {
		worker.interrupt();
		runRef.set(null);
	}
}
