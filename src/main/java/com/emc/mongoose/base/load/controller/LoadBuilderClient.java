package com.emc.mongoose.base.load.controller;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;

import java.rmi.RemoteException;
/**
 Created by andrey on 30.09.14.
 */
public interface LoadBuilderClient<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U> {
	//
	@Override
	LoadBuilderClient<T, U> setProperties(final RunTimeConfig props)
	throws RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setThreadsPerNodeFor(
		final short threadCount, final Request.Type loadType
	) throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setInputFile(final String listFile)
	throws RemoteException;
	//
	@Override
	LoadBuilderClient<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
}
