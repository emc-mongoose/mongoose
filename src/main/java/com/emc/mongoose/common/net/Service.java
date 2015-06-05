package com.emc.mongoose.common.net;
//
import java.io.Closeable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 07.05.14.
 A remote service which has a name for resolution by URI.
 */
public interface Service
extends Remote, Closeable/*, Runnable*/ {
	//
	String getName()
	throws RemoteException;
	//
	void start()
	throws RemoteException;
	//
	void await()
	throws RemoteException, InterruptedException;
	//
	void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException;
}
