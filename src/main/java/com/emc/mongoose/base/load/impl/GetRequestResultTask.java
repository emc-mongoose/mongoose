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
/**
 Created by kurila on 11.12.14.
 */
public final class GetRequestResultTask<T extends DataItem>
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadExecutor<T> executor;
	private final AsyncIOTask<T> ioTask;
	//
	public GetRequestResultTask(final LoadExecutor<T> executor, final AsyncIOTask<T> ioTask) {
		this.executor = executor;
		this.ioTask = ioTask;
	}
	//
	@Override
	public final void run() {
		AsyncIOTask.Result result;
		try {
			ioTask.join();
			result = ioTask.getResult();
		} catch(final CancellationException e) {
			result = AsyncIOTask.Result.FAIL_TIMEOUT;
			LOG.warn(Markers.ERR, "Request has been cancelled:", e);
		} catch(final Exception e) {
			result = AsyncIOTask.Result.FAIL_UNKNOWN;
			ExceptionHandler.trace(LOG, Level.WARN, e, "Unexpected failure");
		}
		executor.handleResult(ioTask, result);
	}
}
