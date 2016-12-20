package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.load.monitor.metrics.IoStats;
import com.emc.mongoose.load.monitor.metrics.IoTraceCsvBatchLogMessage;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
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
			} catch(final InterruptedException e) {
				break;
			}
		}
	}
}
