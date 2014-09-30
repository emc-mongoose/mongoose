package com.emc.mongoose.base.load.driver;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.remote.Service;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 */
public interface LoadBuilderService<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U>, Service {
	//
	@Override
	LoadBuilderService<T, U> setProperties(final RunTimeConfig props)
	throws RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setThreadsPerNodeFor(final short threadCount, final Request.Type loadType)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setInputFile(final String listFile)
	throws RemoteException;
	//
	@Override
	LoadBuilderService<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
	String buildRemotely()
	throws RemoteException;
	//
}
