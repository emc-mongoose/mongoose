package com.emc.mongoose.common.concurrent;

import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 Created on 21.07.16.
 */
public interface Daemon
extends Closeable {
	
	int MAX_DAEMON_SVC_TASKS = 0x100;
	
	Queue<Daemon> UNCLOSED = new ConcurrentLinkedQueue<>();
	
	static void closeAll() {
		synchronized(UNCLOSED) {
			// close all unclosed daemons
			for(final Daemon d : UNCLOSED) {
				try {
					d.close();
				} catch(final IllegalStateException | ConcurrentModificationException ignored) {
				} catch(final Throwable t) {
					t.printStackTrace(System.err);
				}
			}
			
			// wait until the list of the unclosed daemons is empty
			while(!UNCLOSED.isEmpty()) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch(final InterruptedException e) {
					break;
				}
			}
		}
	}
	
	enum State {
		INITIAL, STARTED, SHUTDOWN, INTERRUPTED, CLOSED
	}

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
