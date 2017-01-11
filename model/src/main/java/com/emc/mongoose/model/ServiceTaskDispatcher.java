package com.emc.mongoose.model;

import com.emc.mongoose.common.concurrent.Daemon;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 30.11.16.
 */
public final class ServiceTaskDispatcher
implements Runnable {
	
	private final Map<? extends Daemon, Runnable> dispatchTasks;
	
	public ServiceTaskDispatcher(final Map<? extends Daemon, Runnable> dispatchTasks) {
		this.dispatchTasks = dispatchTasks;
	}
	
	@Override
	public final void run() {
		try {
			while(true) {
				Runnable nextDispatchTask;
				if(dispatchTasks.size() == 0) {
					Thread.sleep(1);
				} else {
					for(final Daemon taskOwner : dispatchTasks.keySet()) {
						nextDispatchTask = dispatchTasks.get(taskOwner);
						if(nextDispatchTask != null) {
							try {
								nextDispatchTask.run();
							} catch(final Exception e) {
								if(
									taskOwner != null && !taskOwner.isInterrupted() &&
									!taskOwner.isClosed()
								) {
									e.printStackTrace(System.err);
								}
							}
						}
						LockSupport.parkNanos(1);
					}
				}
			}
		} catch(final InterruptedException | RemoteException ignored) {
		} finally {
			dispatchTasks.clear();
		}
	}
}
