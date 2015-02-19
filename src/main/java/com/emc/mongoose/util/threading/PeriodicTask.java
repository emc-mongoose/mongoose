package com.emc.mongoose.util.threading;
/**
 Created by kurila on 18.02.15.
 */
public interface PeriodicTask<T>
extends Runnable {
	T getLastResult();
}
