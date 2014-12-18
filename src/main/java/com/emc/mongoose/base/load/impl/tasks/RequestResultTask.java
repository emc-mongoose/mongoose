package com.emc.mongoose.base.load.impl.tasks;
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
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
/**
 Created by kurila on 11.12.14.
 */
public final class RequestResultTask<T extends DataItem>
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadExecutor<T> executor;
	private final AsyncIOTask<T> ioTask;
	private final Future<AsyncIOTask.Result> futureResult;
	//
	public RequestResultTask(
		final LoadExecutor<T> executor, final AsyncIOTask<T> ioTask,
		final Future<AsyncIOTask.Result> futureResult
	) {
		this.executor = executor;
		this.ioTask = ioTask;
		this.futureResult = futureResult;
	}
	//
	@Override
	public final void run() {
		AsyncIOTask.Result ioTaskResult;
		try {
			ioTaskResult = futureResult.get(); // submit done
			executor.handleResult(ioTask, ioTaskResult);
		} catch(final CancellationException e) {
			LOG.warn(Markers.ERR, "Request has been cancelled:", e);
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Request result handling failed");
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Unexpected failure");
		}

	}
}
