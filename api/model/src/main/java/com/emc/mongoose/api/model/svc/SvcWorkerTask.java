package com.emc.mongoose.api.model.svc;

import com.emc.mongoose.api.common.concurrent.Coroutine;
import com.emc.mongoose.api.common.concurrent.Daemon;
import com.emc.mongoose.api.common.concurrent.StoppableTask;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 Created by andrey on 25.07.17.
 */
public final class SvcWorkerTask
implements StoppableTask {

	private final Map<Daemon, List<Coroutine>> allSvcCoroutines;

	private volatile boolean closedFlag = false;

	public SvcWorkerTask(final Map<Daemon, List<Coroutine>> allSvcCoroutines) {
		this.allSvcCoroutines = allSvcCoroutines;
	}

	@Override
	public final void run() {
		Set<Map.Entry<Daemon, List<Coroutine>>> svcCoroutineEntries;
		List<Coroutine> nextSvcCoroutines;
		while(!closedFlag) {
			svcCoroutineEntries = allSvcCoroutines.entrySet();
			if(svcCoroutineEntries.size() == 0) {
				try {
					Thread.sleep(1);
				} catch(final InterruptedException e) {
					break;
				}
			} else {
				for(final Map.Entry<Daemon, List<Coroutine>> entry : svcCoroutineEntries) {
					nextSvcCoroutines = entry.getValue();
					for(final Coroutine nextSvcCoroutine : nextSvcCoroutines) {
						try {
							nextSvcCoroutine.run();
						} catch(final Throwable t) {
							synchronized (System.err) {
								System.err.println(
									entry.getKey().toString() + ": service coroutine \"" +
										nextSvcCoroutine + "\" failed:"
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
