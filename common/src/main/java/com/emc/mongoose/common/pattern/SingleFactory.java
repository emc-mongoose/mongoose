package com.emc.mongoose.common.pattern;

import java.rmi.RemoteException;

/**
 Created on 30.09.16.
 */
public interface SingleFactory<T> {

	T create()
	throws RemoteException;

}
