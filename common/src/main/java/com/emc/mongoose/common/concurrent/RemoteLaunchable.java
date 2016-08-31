package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.exception.UserShootHisFootException;

import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 Created on 21.07.16.
 */
public interface RemoteLaunchable {

	void start()
	throws UserShootHisFootException, RemoteException;

	boolean isStarted()
	throws RemoteException;

	boolean await()
	throws InterruptedException, RemoteException;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException;

}
