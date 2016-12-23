package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
/**
 Created by andrey on 15.12.16.
 */
public final class GetAndProcessIoResultsSvcTask<
	I extends Item, O extends IoTask<I, R>, R extends IoResult
>
implements Runnable {

	private static final Logger LOG = LogManager.getLogger();

	private final LoadMonitor<R> monitor;
	private final StorageDriver<I, O, R> driver;
	private final boolean isCircular;

	public GetAndProcessIoResultsSvcTask(
		final LoadMonitor<R> monitor, final StorageDriver<I, O, R> driver, final boolean isCircular
	) {
		this.monitor = monitor;
		this.driver = driver;
		this.isCircular = isCircular;
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
					if(resultsCount > 0) {
						monitor.processIoResults(results, resultsCount, isCircular);
					}
				}
				Thread.sleep(1);
			} catch(final IOException e) {
				try {
					if(!driver.isClosed()) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to get the results from the driver \"{}\"",
							driver.toString()
						);
						Thread.sleep(1);
					} else {
						break;
					}
				} catch(final RemoteException | InterruptedException ee) {
					LogUtil.exception(LOG, Level.DEBUG, e, "Failure");
					break;
				}
			} catch(final InterruptedException e) {
				break;
			}
		}
	}
}
