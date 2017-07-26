package com.emc.mongoose.api.model.svc;

import com.emc.mongoose.api.common.concurrent.Daemon;
import com.emc.mongoose.api.common.concurrent.SvcTask;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 25.07.17.
 */
public final class SvcWorkerTask
implements SvcTask {

	private final Map<Daemon, List<SvcTask>> allSvcTasks;

	private volatile boolean closedFlag = false;

	public SvcWorkerTask(final Map<Daemon, List<SvcTask>> allSvcTasks) {
		this.allSvcTasks = allSvcTasks;
	}

	@Override
	public final void run() {
		Set<Map.Entry<Daemon, List<SvcTask>>> svcTaskEntries;
		List<SvcTask> nextSvcTasks;
		while(!closedFlag) {
			svcTaskEntries = allSvcTasks.entrySet();
			if(svcTaskEntries.size() == 0) {
				try {
					Thread.sleep(1);
				} catch(final InterruptedException e) {
					break;
				}
			} else {
				for(final Map.Entry<Daemon, List<SvcTask>> entry : svcTaskEntries) {
					nextSvcTasks = entry.getValue();
					for(final SvcTask nextSvcTask : nextSvcTasks) {
						try {
							nextSvcTask.run();
						} catch(final Throwable t) {
							synchronized (System.err) {
								System.err.println(
									entry.getKey().toString() + ": service task \"" + nextSvcTask +
										"\" failed:"
								);
								t.printStackTrace(System.err);
							}
						}
						//LockSupport.parkNanos(1);
					}
				}
			}
		}
	}

	@Override
	public final boolean isClosed() {
		return closedFlag;
	}

	@Override
	public final void close() {
		closedFlag = true;
	}
}
