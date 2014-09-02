package com.emc.mongoose.remote;
//
import java.io.Closeable;
import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 Created by kurila on 07.05.14.
 */
public interface Service
extends Remote, Closeable {
	//
	String getName()
	throws RemoteException;
	//
}
