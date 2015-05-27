package com.emc.mongoose.core.impl.load.tasks;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 11.12.14.
 */
public final class RequestResultTask<T extends DataItem>
implements Runnable, Reusable<RequestResultTask<T>> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int
		reqTimeOutMilliSec = RunTimeConfig.getContext().getRunReqTimeOutMilliSec();
	//
	private volatile LoadExecutor<T> executor = null;
	private volatile IOTask<T> ioTask = null;
	private volatile Future<IOTask.Status> futureResult = null;
	//
	public final static AtomicBoolean IS_LAST_TASK_DONE = new AtomicBoolean(true);
	public final static Lock RESULT_TASKS_LOCK = new ReentrantLock();
	public final static Condition TASKS_COND = RESULT_TASKS_LOCK.newCondition();
	@Override
	public final void run() {
		IOTask.Status ioTaskStatus = IOTask.Status.FAIL_UNKNOWN;
		try {
			IS_LAST_TASK_DONE.compareAndSet(true, false);
			ioTaskStatus = futureResult.get(reqTimeOutMilliSec, TimeUnit.MILLISECONDS);
			IS_LAST_TASK_DONE.compareAndSet(false, true);
			if (IS_LAST_TASK_DONE.get()) {
				if (RESULT_TASKS_LOCK.tryLock(1, TimeUnit.SECONDS)) {
					try {
						TASKS_COND.signalAll();
					} finally {
						RESULT_TASKS_LOCK.unlock();
					}
				} else {
					LOG.debug(LogUtil.ERR, "Failed to acquire lock to request result task");
				}
			}
		} catch(final InterruptedException | CancellationException e) {
			LogUtil.failure(LOG, Level.TRACE, e, "Request has been cancelled");
		} catch(final ExecutionException e) {
			LogUtil.failure(LOG, Level.DEBUG, e, "Task execution failure: #" + ioTask.hashCode());
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.WARN, e, "Unexpected failure");
		} finally {
			try {
				executor.handleResult(ioTask, ioTaskStatus);
			} catch(final IOException e) {
				LogUtil.failure(LOG, Level.DEBUG, e, "Request result handling failed");
			}
			//
			release();
		}
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(
				LogUtil.MSG, "Task #{} done w/ result {}",
				ioTask.hashCode(), ioTaskStatus.name()
			);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<RequestResultTask>
		POOL = new InstancePool<>(RequestResultTask.class);
	//
	public static RequestResultTask<? extends DataItem> getInstance(
		final LoadExecutor<? extends DataItem> executor,
		final IOTask<? extends DataItem> ioTask,
		final Future<IOTask.Status> futureResult
	) throws InterruptedException {
		return (RequestResultTask<? extends DataItem>) POOL.take(executor, ioTask, futureResult);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final RequestResultTask<T> reuse(final Object... args)
	throws IllegalArgumentException {
		if(args == null) {
			throw new IllegalArgumentException("No arguments for reusing the instance");
		}
		if(args.length > 0) {
			executor = (LoadExecutor<T>) args[0];
			if(executor == null) {
				throw new IllegalArgumentException("No executor is specified");
			}
		}
		if(args.length > 1) {
			ioTask = (IOTask<T>) args[1];
			if(ioTask == null) {
				throw new IllegalArgumentException("I/O task shouldn't be null");
			}
		}
		if(args.length > 2) {
			futureResult = (Future<IOTask.Status>) args[2];
			if(futureResult == null) {
				throw new IllegalArgumentException("Result future shouldn't be null");
			}
		}
		return this;
	}
	//
	@Override
	public final void release() {
		if(ioTask != null) {
			ioTask.release();
		}
		POOL.release(this);
	}
}
