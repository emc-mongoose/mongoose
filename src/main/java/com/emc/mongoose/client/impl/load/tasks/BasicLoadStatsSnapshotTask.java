package com.emc.mongoose.client.impl.load.tasks;

import com.emc.mongoose.client.api.load.tasks.LoadStatsSnapshotTask;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.load.model.metrics.IoStats;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.ConnectIOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 28.06.16.
 */
public class BasicLoadStatsSnapshotTask
implements LoadStatsSnapshotTask {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final LoadSvc loadSvc;
	private final String loadSvcAddr;
	protected volatile IoStats.Snapshot lastSnapshot = null;
	//
	public BasicLoadStatsSnapshotTask(final LoadSvc loadSvc, final String loadSvcAddr) {
		this.loadSvc = loadSvc;
		this.loadSvcAddr = loadSvcAddr;
	}
	//
	@Override
	public final IoStats.Snapshot getLastStatsSnapshot() {
		return lastSnapshot;
	}
	//
	protected void refreshLastStatsSnapshot()
	throws RemoteException {
		lastSnapshot = loadSvc.getStatsSnapshot();
	}
	//
	@Override
	public final void run() {
		final Thread currThread = Thread.currentThread();
		currThread.setName(currThread.getName() + "@" + loadSvcAddr);
		int retryCount = 0;
		while(!currThread.isInterrupted()) {
			try {
				refreshLastStatsSnapshot();
				retryCount = 0; // reset
				if(lastSnapshot != null) {
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Got metrics snapshot from {}: {}",
							loadSvcAddr, lastSnapshot
						);
					}
				} else {
					LOG.warn(
						Markers.ERR, "Got null metrics snapshot from the load server @ {}",
						loadSvcAddr
					);
				}
				LockSupport.parkNanos(1_000_000);
			} catch(final NoSuchObjectException | ConnectIOException e) {
				if(retryCount < COUNT_LIMIT_RETRIES) {
					retryCount ++;
					LogUtil.exception(
						LOG, Level.DEBUG, e,
						"Failed to fetch the metrics snapshot from {} {} times",
						loadSvcAddr, retryCount
					);
				} else {
					LogUtil.exception(
						LOG, Level.ERROR, e,
						"Failed to fetch the metrics from {} {} times, stopping the task",
						loadSvcAddr, retryCount
					);
					break;
				}
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e,
					"Failed to fetch the metrics snapshot from {}", loadSvcAddr
				);
			}
		}
	}
}
