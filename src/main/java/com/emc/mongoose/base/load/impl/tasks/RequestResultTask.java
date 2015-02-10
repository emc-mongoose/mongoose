package com.emc.mongoose.base.load.impl.tasks;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.collections.InstancePool;
import com.emc.mongoose.util.collections.Reusable;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 11.12.14.
 */
public final class RequestResultTask<T extends DataItem>
implements Runnable, Reusable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile LoadExecutor<T> executor = null;
	private volatile AsyncIOTask<T> ioTask = null;
	private volatile Future<AsyncIOTask.Status> futureResult = null;
	//
	@Override
	public final void run() {
		AsyncIOTask.Status ioTaskStatus = AsyncIOTask.Status.FAIL_UNKNOWN;
		try {
			ioTaskStatus = futureResult.get(); // submit done
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Task #{} done w/ result {}",
					ioTask.hashCode(), ioTaskStatus.name()
				);
			}
		} catch(final InterruptedException | CancellationException e) {
			TraceLogger.failure(LOG, Level.TRACE, e, "Request has been cancelled");
		} catch(final ExecutionException e) {
			TraceLogger.failure(
				LOG, Level.DEBUG, e,
				String.format(
					"Request \"%s\" to \"%s\" execution failure",
					ioTask, ioTask.getNodeAddr()
				)
			);
		} catch(final Exception e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Unexpected failure");
		}
		//
		if(executor != null) {
			try {
				executor.handleResult(ioTask, ioTaskStatus);
			} catch(final IOException e) {
				TraceLogger.failure(LOG, Level.DEBUG, e, "Request result handling failed");
			}
		}
		//
		release();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<RequestResultTask>
		POOL = new InstancePool<>(RequestResultTask.class);
	//
	public static RequestResultTask<? extends DataItem> getInstance(
		final LoadExecutor<? extends DataItem> executor,
		final AsyncIOTask<? extends DataItem> ioTask,
		final Future<AsyncIOTask.Status> futureResult
	) throws InterruptedException {
		return (RequestResultTask<? extends DataItem>) POOL.take(executor, ioTask, futureResult);
	}
	//
	private final AtomicBoolean isAvailable = new AtomicBoolean(true);
	//
	@Override @SuppressWarnings("unchecked")
	public final RequestResultTask<T> reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(isAvailable.compareAndSet(true, false)) {
			if(args==null) {
				throw new IllegalArgumentException("No arguments for reusing the instance");
			}
			if(args.length > 0) {
				executor = (LoadExecutor<T>) args[0];
			}
			if(args.length > 1) {
				ioTask = (AsyncIOTask<T>) args[1];
				if(ioTask == null) {
					throw new IllegalArgumentException("I/O task shouldn't be null");
				}
			}
			if(args.length > 2) {
				futureResult = (Future<AsyncIOTask.Status>) args[2];
				if(futureResult == null) {
					throw new IllegalArgumentException("I/O task future result shouldn't be null");
				}
			}
		} else {
			throw new IllegalStateException("Not yet released instance reuse attempt");
		}
		return this;
	}
	//
	@Override
	public final void release() {
		if(isAvailable.compareAndSet(false, true)) {
			if(ioTask != null) {
				ioTask.release();
			}
			POOL.release(this);
		}
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int compareTo(Reusable another) {
		return another == null ? 1 : hashCode() - another.hashCode();
	}
}
