package com.emc.mongoose.api.model.concurrent;

import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created on 21.07.16.
 */
public interface Daemon
extends Closeable {

	List<Coroutine> getSvcCoroutines()
	throws RemoteException;
	
	enum State {
		INITIAL, STARTED, SHUTDOWN, INTERRUPTED, CLOSED
	}

	State getState()
	throws RemoteException;

	void start()
	throws IllegalStateException, RemoteException;

	boolean isStarted()
	throws RemoteException;
	
	void shutdown()
	throws IllegalStateException, RemoteException;
	
	boolean isShutdown()
	throws RemoteException;
	
	void await()
	throws InterruptedException, RemoteException;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException;
	
	void interrupt()
	throws IllegalStateException, RemoteException;
	
	boolean isInterrupted()
	throws RemoteException;
	
	boolean isClosed()
	throws RemoteException;
}
