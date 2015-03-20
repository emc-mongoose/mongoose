package com.emc.mongoose.client.impl.load.executor.tasks;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
import com.emc.mongoose.core.impl.util.log.TraceLogger;
import com.emc.mongoose.core.api.util.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
/**
 Created by kurila on 23.12.14.
 */
public final class RemoteJoinTask
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadSvc loadSvc;
	private final long timeOutMilliSec;
	//
	public RemoteJoinTask(final LoadSvc loadSvc, final long timeOutMilliSec) {
		this.loadSvc = loadSvc;
		this.timeOutMilliSec = timeOutMilliSec;
	}
	//
	@Override
	public final void run() {
		try {
			try {
				loadSvc.join(timeOutMilliSec);
			} catch(final InterruptedException e) {
				LOG.debug(
					Markers.MSG, "Remote join task for \"{}\" was interrupted", loadSvc.getName()
				);
			}
		} catch(final NoSuchObjectException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Remote join failed, no such service");
		} catch(final RemoteException e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Remote join task failure");
		}
	}
}
