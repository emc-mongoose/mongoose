package com.emc.mongoose.base.concurrent;

public interface SingleTaskExecutor extends TaskExecutor {

	/** @return the active task instance, null if no active task is being executed at the moment */
	Runnable task();

	/**
	* Atomically stop the active task
	*
	* @param task the task to check if it is still active
	* @return true if the task was still active and stopped, false otherwise
	*/
	boolean stop(final Runnable task);
}
