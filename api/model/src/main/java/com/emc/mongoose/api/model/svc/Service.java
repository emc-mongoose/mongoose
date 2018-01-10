package com.emc.mongoose.api.model.svc;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 Created by kurila on 07.05.14.
 A remote service which has a name for resolution by URI.
 */
public interface Service
extends Remote {

	int getRegistryPort()
	throws RemoteException;

	String getName()
	throws RemoteException;
}
