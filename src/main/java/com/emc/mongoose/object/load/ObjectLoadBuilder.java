package com.emc.mongoose.object.load;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.load.LoadBuilder;
//
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.util.conf.RunTimeConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
/**
 Created by kurila on 29.09.14.
 */
public interface ObjectLoadBuilder<T extends DataObject, U extends ObjectLoadExecutor<T>>
extends LoadBuilder<T, U> {
	//
	@Override
	ObjectLoadBuilder<T, U> setProperties(final RunTimeConfig props)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setThreadsPerNodeFor(final short threadCount, final Request.Type loadType)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setInputFile(final String listFile)
	throws RemoteException;
	//
	@Override
	ObjectLoadBuilder<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
	@Override
	U build()
	throws URISyntaxException, IOException;
}
