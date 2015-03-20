package com.emc.mongoose.client.api.load.executor.tasks;
/**
 Created by kurila on 18.02.15.
 */
public interface PeriodicTask<T>
extends Runnable {
	T getLastResult();
}
