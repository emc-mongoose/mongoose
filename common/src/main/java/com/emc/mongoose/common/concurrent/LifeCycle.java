package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.io.value.async.Initializable;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 23.09.15.
 */
public interface LifeCycle
extends Initializable {

	void start()
	throws IllegalStateException;

	boolean isStarted();

	void shutdown()
	throws IllegalStateException;

	boolean isShutdown();

	void interrupt()
	throws IllegalStateException;

	boolean isInterrupted();

	boolean await()
	throws InterruptedException;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException;
}
