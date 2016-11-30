package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 Created by kurila on 30.11.16.
 */
public final class CommonDispatchTask
implements Runnable {
	
	private static final Logger LOG = LogManager.getLogger();
	
	private final Map<String, Runnable> dispatchTasks;
	
	public CommonDispatchTask(final Map<String, Runnable> dispatchTasks) {
		this.dispatchTasks = dispatchTasks;
	}
	
	@Override
	public final void run() {
		try {
			while(true) {
				Runnable nextDispatchTask;
				for(final String taskOwnerName : dispatchTasks.keySet()) {
					nextDispatchTask = dispatchTasks.get(taskOwnerName);
					if(nextDispatchTask != null) {
						try {
							nextDispatchTask.run();
						} catch(final Exception e) {
							LogUtil.exception(
								LOG, Level.WARN, e,
								"Failed to invoke the I/O task dispatching for the \"{}\"",
								taskOwnerName
							);
						}
						Thread.sleep(1);
					}
				}
			}
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted");
		} finally {
			dispatchTasks.clear();
		}
	}
}
