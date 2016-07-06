package com.emc.mongoose.client.impl.load.tasks;

import com.emc.mongoose.server.api.load.executor.LoadSvc;

import java.rmi.RemoteException;

/**
 Created by kurila on 28.06.16.
 */
public class LoadIntermediateStatsSnapshotTask
extends BasicLoadStatsSnapshotTask {
	
	public LoadIntermediateStatsSnapshotTask(final LoadSvc loadSvc, final String loadSvcAddr) {
		super(loadSvc, loadSvcAddr);
	}

	protected void refreshLastStatsSnapshot()
	throws RemoteException {
		lastSnapshot = loadSvc.getIntermediateStatsSnapshot();
	}
}
