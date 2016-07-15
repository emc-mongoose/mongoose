package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.exception.UserShootItsFootException;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 23.09.15.
 */
public interface LifeCycle {

	void start()
	throws UserShootItsFootException;

	boolean isStarted();

	void shutdown()
	throws UserShootItsFootException;

	boolean isShutdown();

	void interrupt()
	throws UserShootItsFootException;

	boolean isInterrupted();

	boolean await()
	throws InterruptedException;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException;
}
