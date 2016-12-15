package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.io.collection.IoBuffer;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 15.12.16.
 */
public final class GetIoResultsSvcTask<
	I extends Item, O extends IoTask<I, R>, R extends IoTask.IoResult
>
implements Runnable {

	private static final Logger LOG = LogManager.getLogger();

	private final StorageDriver<I, O, R> driver;
	private final IoBuffer<R> queue;

	public GetIoResultsSvcTask(final StorageDriver<I, O, R> driver, final IoBuffer<R> queue) {
		this.driver = driver;
		this.queue = queue;
	}

	@Override
	public final void run() {
		List<R> results;
		int resultsCount;
		final Thread currThread = Thread.currentThread();
		currThread.setName(currThread.getName() + "-" + "getIoResults");
		while(!currThread.isInterrupted()) {
			try {
				results = driver.getResults();
				if(results != null) {
					resultsCount = results.size();
					for(int i = 0; i < resultsCount; i += queue.put(results, i, resultsCount)) {
						LockSupport.parkNanos(1);
					}
				}
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to get the results from the driver \"{}\"",
					driver.toString()
				);
				try {
					if(driver.isInterrupted() || driver.isClosed()) {
						break;
					} else {
						Thread.sleep(1);
					}
				} catch(final RemoteException | InterruptedException ee) {
					LogUtil.exception(LOG, Level.WARN, e, "Failure");
					break;
				}
			}
		}
	}
}
