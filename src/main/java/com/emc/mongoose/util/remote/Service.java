package com.emc.mongoose.util.remote;
//
import java.io.Closeable;
import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 Created by kurila on 07.05.14.
 A remote service which has a name for resolution by URI.
 */
public interface Service
extends Remote, Closeable/*, Runnable*/ {
	//
	void start()
	throws RemoteException;
	//
	String getName()
	throws RemoteException;
	//
}
