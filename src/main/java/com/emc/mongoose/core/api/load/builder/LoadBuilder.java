package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
// mongoose-common
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import java.io.Closeable;
import java.io.IOException;
import java.rmi.RemoteException;
/**
 Created by kurila on 28.04.14.
 A builder pattern implementation which should help to instantiate a configured load executor.
 */
public interface LoadBuilder<T extends DataItem, U extends LoadExecutor<T>>
extends Closeable {
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
	LoadBuilder<T, U> setLoadType(final IOTask.Type loadType)
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
	LoadBuilder<T, U> setManualTaskSleepMicroSecs(final int manualTaskSleepMicroSec)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setRateLimit(final float rateLimit)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setWorkerCountDefault(final int threadCount)
	throws RemoteException;
	//
	LoadBuilder<T, U> setWorkerCountFor(final int threadCount, final IOTask.Type loadType)
	throws RemoteException;
	//
	LoadBuilder<T, U> setConnPerNodeDefault(final int connCount)
		throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setConnPerNodeFor(final int connCount, final IOTask.Type loadType)
		throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setUpdatesPerItem(final int count)
	throws RemoteException;
	//
	LoadBuilder<T, U> setItemSrc(final ItemSrc<T> itemSrc)
	throws RemoteException;
	//
	LoadBuilder<T, U> useNewItemSrc()
	throws RemoteException;
	//
	LoadBuilder<T, U> useNoneItemSrc()
	throws RemoteException;
	//
	LoadBuilder<T, U> useContainerListingItemSrc()
	throws RemoteException;
	//
	U build()
	throws IOException;
	//
}
