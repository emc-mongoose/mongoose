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
	DataLoadBuilder<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	DataLoadBuilder<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	DataLoadBuilder<T, U> setObjSizeBias(final float objSizeBias)
	throws IllegalArgumentException, RemoteException;
	//
	DataLoadBuilder<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
	DataLoadBuilder<T, U> useContainerListingItemSrc()
	throws RemoteException;
	//
}
