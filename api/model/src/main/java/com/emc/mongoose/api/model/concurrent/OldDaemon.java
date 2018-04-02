package com.emc.mongoose.api.model.concurrent;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 Created on 21.07.16.
 A stateful execution entity. Usually contains the list of the concurrent service tasks implemented
 as coroutines.
 */
public interface OldDaemon
extends Closeable {

	enum State {
		INITIAL, STARTED, SHUTDOWN, INTERRUPTED, CLOSED
	}

	State getState();

	void start()
	throws IllegalStateException;

	boolean isStarted();
	
	void shutdown()
	throws IllegalStateException;
	
	boolean isShutdown();
	
	void await()
	throws InterruptedException;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException;
	
	void interrupt()
	throws IllegalStateException;
	
	boolean isInterrupted();
	
	boolean isClosed();
}
