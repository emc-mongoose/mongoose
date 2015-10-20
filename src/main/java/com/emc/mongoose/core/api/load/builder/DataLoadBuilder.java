package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;

import java.rmi.RemoteException;
/**
 Created by kurila on 20.10.15.
 */
public interface DataLoadBuilder<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U> {
	//
	LoadBuilder<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setObjSizeBias(final float objSizeBias)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
	LoadBuilder<T, U> useContainerListingItemSrc()
	throws RemoteException;
	//
}
