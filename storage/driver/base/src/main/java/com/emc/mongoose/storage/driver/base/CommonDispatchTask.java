package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import java.util.Map;

/**
 Created by kurila on 30.11.16.
 */
public final class CommonDispatchTask
implements Runnable {
	
	private static final Logger LOG = LogManager.getLogger();
	
	private final Map<StorageDriver, Runnable> dispatchTasks;
	
	public CommonDispatchTask(final Map<StorageDriver, Runnable> dispatchTasks) {
		this.dispatchTasks = dispatchTasks;
	}
	
	@Override
	public final void run() {
		try {
			while(true) {
				Runnable nextDispatchTask;
				for(final StorageDriver taskOwner : dispatchTasks.keySet()) {
					nextDispatchTask = dispatchTasks.get(taskOwner);
					if(nextDispatchTask != null) {
						try {
							nextDispatchTask.run();
						} catch(final Exception e) {
							if(
								taskOwner != null && !taskOwner.isInterrupted() &&
								!taskOwner.isClosed()
							) {
								LogUtil.exception(
									LOG, Level.WARN, e,
									"Failed to invoke the I/O task dispatching for the \"{}\"",
									taskOwner
								);
							}
						}
						Thread.sleep(1);
					}
				}
			}
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		} catch(final RemoteException ignored) {
		} finally {
			dispatchTasks.clear();
		}
	}
}
