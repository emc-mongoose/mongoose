package com.emc.mongoose.common.concurrent;
//
import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 23.09.15.
 */
public interface LifeCycle
extends Closeable {
	//
	void start()
	throws RemoteException, IllegalThreadStateException;
	//
	void shutdown()
	throws RemoteException, IllegalStateException;
	//
	void await()
	throws RemoteException, InterruptedException;
	//
	void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException;
	//
	void interrupt()
	throws RemoteException;
}
