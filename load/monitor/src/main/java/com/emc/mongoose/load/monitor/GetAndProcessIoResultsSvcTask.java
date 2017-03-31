package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.io.task.IoTask;
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
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 15.12.16.
 */
public final class GetAndProcessIoResultsSvcTask<I extends Item, O extends IoTask<I>>
implements Runnable {

	private static final Logger LOG = LogManager.getLogger();

	private final LoadMonitor<I, O> monitor;
	private final StorageDriver<I, O> driver;
	private final Set<Runnable> svcTasks;

	public GetAndProcessIoResultsSvcTask(
		final LoadMonitor<I, O> monitor, final StorageDriver<I, O> driver,
		final Set<Runnable> svcTasks
	) {
		this.monitor = monitor;
		this.driver = driver;
		this.svcTasks = svcTasks;
	}

	@Override
	public final void run() {
		try {
			final List<O> results = driver.getResults();
			LockSupport.parkNanos(1);
			if(results != null) {
				final int resultsCount = results.size();
				if(resultsCount > 0) {
					monitor.processIoResults(results, resultsCount);
				}
			}
		} catch(final IOException e) {
			try {
				if(!driver.isClosed()) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to get the results from the driver \"{}\"",
						driver.toString()
					);
					Thread.sleep(1);
				} else {
					svcTasks.remove(this);
				}
			} catch(final RemoteException | InterruptedException ee) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failure");
				svcTasks.remove(this);
			}
		}
	}
}
