package com.emc.mongoose.base.load.impl.tasks;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.pool.InstancePool;
import com.emc.mongoose.util.pool.Reusable;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
	private volatile Future<AsyncIOTask.Result> futureResult = null;
	//
	@Override
	public final void run() {
		AsyncIOTask.Result ioTaskResult;
		try {
			ioTaskResult = futureResult.get(); // submit done
			executor.handleResult(ioTask, ioTaskResult);
		} catch(final InterruptedException | CancellationException e) {
			ExceptionHandler.trace(LOG, Level.TRACE, e, "Request has been cancelled");
		} catch(final ExecutionException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Request execution failure");
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.DEBUG, e, "Request result handling failed");
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Unexpected failure");
		} finally {
			close();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<RequestResultTask>
		POOL = new InstancePool<>(RequestResultTask.class);
	//
	public static RequestResultTask<? extends DataItem> getInstance(
		final LoadExecutor<? extends DataItem> executor,
		final AsyncIOTask<? extends DataItem> ioTask,
		final Future<AsyncIOTask.Result> futureResult
	) {
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
		}
		if(args.length > 1) {
			ioTask = (AsyncIOTask<T>) args[1];
		}
		if(args.length > 2) {
			futureResult = (Future<AsyncIOTask.Result>) args[2];
		}
		return this;
	}
	//
	@Override
	public final void close() {
		POOL.release(this);
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int compareTo(Reusable another) {
		return another == null ? 1 : hashCode() - another.hashCode();
	}
}
