package com.emc.mongoose.common.pattern;

import java.rmi.RemoteException;

/**
 Created by on 9/21/2016.
 */
public interface EnumFactory<T, E extends Enum<E>>
extends Factory<T, E> {

	T create(final E type)
	throws RemoteException;
}
