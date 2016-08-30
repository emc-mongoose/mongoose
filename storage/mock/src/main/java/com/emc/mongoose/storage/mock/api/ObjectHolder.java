package com.emc.mongoose.storage.mock.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 Created on 30.08.16.
 */
public interface ObjectHolder<T extends MutableDataItemMock> extends Remote {

	String SERVICE_NAME = "objectGet";

	T getObject(final String containerName, final String id, final long offset, final long size)
	throws RemoteException;

}
