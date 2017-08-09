package com.emc.mongoose.api.model.concurrent;

import com.emc.mongoose.api.common.concurrent.StoppableTask;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 Created by andrey on 25.07.17.
 */
public final class CoroutinesProcessorTask
implements StoppableTask {

	private final Map<Daemon, List<Coroutine>> coroutines;
	private volatile boolean closedFlag = false;

	public CoroutinesProcessorTask(final Map<Daemon, List<Coroutine>> coroutines) {
		this.coroutines = coroutines;
	}

	@Override
	public final void run() {
		Set<Map.Entry<Daemon, List<Coroutine>>> coroutineEntries;
		List<Coroutine> nextCoroutines;
		while(!closedFlag) {
			coroutineEntries = coroutines.entrySet();
			if(coroutineEntries.size() == 0) {
				try {
					Thread.sleep(1);
				} catch(final InterruptedException e) {
					break;
				}
			} else {
				for(final Map.Entry<Daemon, List<Coroutine>> entry : coroutineEntries) {
					nextCoroutines = entry.getValue();
					for(final Coroutine nextCoroutine : nextCoroutines) {
						try {
							nextCoroutine.run();
						} catch(final Throwable t) {
							synchronized(System.err) {
								System.err.println(
									entry.getKey().toString() + ": coroutine \"" + nextCoroutine +
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
