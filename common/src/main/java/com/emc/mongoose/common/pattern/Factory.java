package com.emc.mongoose.common.pattern;

import java.rmi.RemoteException;

/**
 Created by on 9/21/2016.
 */
public interface Factory<T, S> {

	T create(final S selector)
	throws RemoteException;
}
