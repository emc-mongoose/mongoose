package com.emc.mongoose.base.load.impl;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
//
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import com.emc.mongoose.util.logging.ExceptionHandler;
//
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 09.10.14.
 */
public final class SubmitRequestTask<T extends DataItem, U extends LoadExecutor<T>>
implements Callable<AsyncIOTask<T>> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final T dataItem;
	private final U executor;
	private final RequestConfig<T> reqConfig;
	//
	public SubmitRequestTask(
		final T dataItem, final U executor, final RequestConfig<T> reqConfig
	) {
		this.dataItem = dataItem;
		this.executor = executor;
		this.reqConfig = reqConfig;
	}
	//
	@Override
	public final AsyncIOTask<T> call() {
		final AsyncIOTask<T> ioTask = reqConfig.getRequestFor(dataItem);
		final Future<AsyncIOTask<T>> futureSubmitResult =  executor.submit(ioTask);
		try {
			futureSubmitResult.get();
			executor.submitResultHandling(ioTask);
		} catch(final ExecutionException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Submit failure");
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		}
		return ioTask;
	}
}
