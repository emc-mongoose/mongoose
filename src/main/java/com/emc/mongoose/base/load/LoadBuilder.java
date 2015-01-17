package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import java.io.IOException;
import java.rmi.RemoteException;
/**
 Created by kurila on 28.04.14.
 A builder pattern implementation which should help to instantiate a configured load executor.
 */
public interface LoadBuilder<T extends DataItem, U extends LoadExecutor<T>> {
	//
	String
		MSG_TMPL_NOT_SPECIFIED = "\"{}\" parameter is not specified nor in configuration files neither in command line",
		MSG_TMPL_INVALID_VALUE = "illegal value specified for \"{}\" parameter: {}";
	//
	LoadBuilder<T, U> setProperties(final RunTimeConfig props)
	throws IllegalStateException, RemoteException;
	//
	RequestConfig<T> getRequestConfig()
	throws RemoteException;
	LoadBuilder<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws RemoteException;
	//
	LoadBuilder<T, U> setLoadType(final AsyncIOTask.Type loadType)
	throws IllegalStateException, RemoteException;
	//
	LoadBuilder<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException;
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
	LoadBuilder<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setThreadsPerNodeFor(final short threadCount, final AsyncIOTask.Type loadType)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
	LoadBuilder<T, U> setInputFile(final String listFile)
		throws RemoteException;
	//
	U build()
	throws IOException;
	//
	DataItemBuffer<T> newDataItemBuffer()
	throws IOException;
	//
	long getMaxCount()
	throws RemoteException;
	//
}
