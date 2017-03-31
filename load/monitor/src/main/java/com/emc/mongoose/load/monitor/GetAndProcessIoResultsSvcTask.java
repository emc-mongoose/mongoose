package com.emc.mongoose.load.monitor;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 15.12.16.
 */
public final class GetAndProcessIoResultsSvcTask<I extends Item, O extends IoTask<I>>
implements Closeable, Runnable {

	private static final Logger LOG = LogManager.getLogger();

	private final LoadMonitor<I, O> monitor;
	private final List<StorageDriver<I, O>> drivers;
	private final int driversCount;
	private final AtomicLong rrc = new AtomicLong();

	public GetAndProcessIoResultsSvcTask(
		final LoadMonitor<I, O> monitor, final List<StorageDriver<I, O>> drivers
	) {
		this.monitor = monitor;
		this.drivers = drivers;
		this.driversCount = drivers.size();
	}

	@Override
	public final void run() {
		final StorageDriver<I, O> driver = drivers.get(
			(int) (rrc.getAndIncrement() % driversCount)
		);
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
					close();
				}
			} catch(final RemoteException | InterruptedException ee) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Failure");
				close();
			}
		}
	}

	@Override
	public final void close() {
		try {
			monitor.getSvcTasks().remove(this);
		} catch(final RemoteException ignored) {
		} finally {
			drivers.clear();
		}
	}
}
