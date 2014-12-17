package com.emc.mongoose.base.load.impl.tasks;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 09.10.14.
 */
public final class RequestSubmitTask<T extends DataItem, U extends LoadExecutor<T>>
implements Callable<AsyncIOTask<T>> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final T dataItem;
	private final U executor;
	private final RequestConfig<T> reqConfig;
	//
	public RequestSubmitTask(
		final T dataItem, final U executor, final RequestConfig<T> reqConfig
	) {
		this.dataItem = dataItem;
		this.executor = executor;
		this.reqConfig = reqConfig;
	}
	//
	@Override
	public final AsyncIOTask<T> call() {
		final AsyncIOTask<T> ioTask = reqConfig.getRequestFor(dataItem, executor.getNextNodeAddr());
		try {
			executor.submit(ioTask).get();
			executor.submitResultHandling(ioTask);
		} catch(final ExecutionException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Submit failure");
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Submit result handling failure");
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		}
		return ioTask;
	}
}
