package com.emc.mongoose.base.load.impl;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
/**
 Created by kurila on 11.12.14.
 */
public final class GetRequestResultTask<T extends DataItem>
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadExecutor<T> executor;
	private final AsyncIOTask<T> task;
	private final Future<AsyncIOTask.Result> futureResult;
	//
	public GetRequestResultTask(
		final LoadExecutor<T> executor,
		final AsyncIOTask<T> task, final Future<AsyncIOTask.Result> futureResult
	) {
		this.executor = executor;
		this.task = task;
		this.futureResult = futureResult;
	}
	//
	@Override
	public final void run() {
		AsyncIOTask.Result result = AsyncIOTask.Result.FAIL_UNKNOWN;
		try {
			result = futureResult.get();
		} catch(final InterruptedException e) {
			result = AsyncIOTask.Result.FAIL_TIMEOUT;
			LOG.trace(Markers.ERR, "Interrupted while waiting for the response");
		} catch(final CancellationException e) {
			result = AsyncIOTask.Result.FAIL_TIMEOUT;
			LOG.warn(Markers.ERR, "Request has been cancelled:", e);
		} catch(final ExecutionException e) {
			final Throwable cause = e.getCause();
			if(InterruptedException.class.isInstance(cause)) {
				LOG.trace(Markers.MSG, "Poisoned");
					/*try {
						consumer.submit(null); // pass the poison through the consumer-producer chain
					} catch(final RemoteException ee) {
						LOG.debug(Markers.ERR, "Failed to feed the poison to consumer due to {}", ee.toString());
					}*/
			} else {
				ExceptionHandler.trace(
					LOG, Level.WARN, cause, "Unhandled request execution failure"
				);
				result = AsyncIOTask.Result.FAIL_UNKNOWN;
			}
		} catch(final Exception e) {
			result = AsyncIOTask.Result.FAIL_UNKNOWN;
			ExceptionHandler.trace(LOG, Level.WARN, e, "Unexpected failure");
		}
		// dispatch depending on the result
		executor.dispatch(task, result);
	}
}
