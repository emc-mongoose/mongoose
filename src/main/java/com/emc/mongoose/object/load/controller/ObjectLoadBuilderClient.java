package com.emc.mongoose.object.load.controller;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.load.controller.LoadBuilderClient;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;

import java.rmi.RemoteException;
/**
 Created by andrey on 30.09.14.
 */
public interface ObjectLoadBuilderClient<T extends DataObject, U extends ObjectLoadExecutor<T>>
extends ObjectLoadBuilder<T, U>, LoadBuilderClient<T, U> {
	//
	@Override
	ObjectLoadBuilderClient<T, U> setProperties(final RunTimeConfig props)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setThreadsPerNodeFor(
		final short threadCount, final Request.Type loadType
	) throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setInputFile(final String listFile)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilderClient<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
}
