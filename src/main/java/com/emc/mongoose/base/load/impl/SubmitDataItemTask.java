package com.emc.mongoose.base.load.impl;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.logging.ExceptionHandler;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.10.14.
 */
public final class SubmitDataItemTask<T extends DataItem, U extends LoadExecutor<T>>
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final T dataItem;
	private final U executor;
	//
	public
	SubmitDataItemTask(final T dataItem, final U executor) {
		this.dataItem = dataItem;
		this.executor = executor;
	}
	//
	@Override
	public final void run() {
		try {
			executor.submit(dataItem);
		} catch(final RemoteException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to submit data item");
		}
	}
}
