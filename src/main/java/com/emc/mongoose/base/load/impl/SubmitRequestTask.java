package com.emc.mongoose.base.load.impl;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
//
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
//import com.emc.mongoose.util.logging.ExceptionHandler;
//
//import org.apache.logging.log4j.Level;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
/**
 Created by kurila on 09.10.14.
 */
public final class SubmitRequestTask<T extends DataItem, U extends LoadExecutor<T>>
implements Callable<Future<AsyncIOTask.Result>> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	private final AsyncIOTask<T> request;
	private final U executor;
	//
	public SubmitRequestTask(final AsyncIOTask<T> request, final U executor) {
		this.request = request;
		this.executor = executor;
	}
	//
	@Override
	public final Future<AsyncIOTask.Result> call() {
		return executor.submit(request);
	}
}
