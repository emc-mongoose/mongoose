package com.emc.mongoose.api.model.concurrent;

import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

public interface AsyncRunnable
extends Closeable {

	enum State {
		INITIAL, STARTED, STOPPED, FINISHED
	}

	/**
	 @return the current state
	 */
	State state()
	throws RemoteException;

	/**
	 @return true if the state is "initial", false otherwise
	 */
	boolean isInitial()
	throws RemoteException;

	/**
	 @return true if the state is "started", false otherwise
	 */
	boolean isStarted()
	throws RemoteException;

	/**
	 @return true if the state is "stopped", false otherwise
	 */
	boolean isStopped()
	throws RemoteException;

	/**
	 @return true if the state is "finished", false otherwise
	 */
	boolean isFinished()
	throws RemoteException;

	/**
	 Start/resume the execution
	 @return the same instance with state changed to <i>STARTED</i> if call was successful.
	 @throws IllegalStateException if the previous state is not <i>INITIAL</i> neither <i>STOPPED</i>
	 */
	AsyncRunnable start()
	throws IllegalStateException, RemoteException;

	/**
	 Stop (with further resumption capability)
	 @return the same instance with state changed to <i>STOPPED</i> if call was successful
	 @throws IllegalStateException if the previous state is not <i>STARTED</i>
	 */
	AsyncRunnable stop()
	throws IllegalStateException, RemoteException;

	/**
	 Wait while the state is <i>STARTED</i>
	 @return the same instance
	 @throws InterruptedException
	 */
	AsyncRunnable await()
	throws InterruptedException, RemoteException;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException;
}
