package com.emc.mongoose;
//
import com.emc.mongoose.api.Request;
import com.emc.mongoose.api.RequestConfig;
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.UniformData;
import com.emc.mongoose.data.UniformDataSource;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 28.04.14.
 */
public interface LoadBuilder<T extends LoadExecutor<? extends UniformData>>
extends Builder<T> {
	//
	LoadBuilder<T> setProperties(final RunTimeConfig props)
	throws RemoteException;
	//
	LoadBuilder<T> setRequestConfig(final RequestConfig<?> reqConf)
	throws RemoteException;
	//
	LoadBuilder<T> setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException;
	//
	LoadBuilder<T> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T> setThreadsPerNodeFor(final short threadCount, final Request.Type loadType)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T> setInputFile(final String listFile)
	throws RemoteException;
	//
	LoadBuilder<T> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
	long getMaxCount()
	throws RemoteException;
	//
	UniformDataSource getDataSource()
	throws RemoteException;
}
