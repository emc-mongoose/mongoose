package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import java.io.Closeable;
import java.io.IOException;
import java.rmi.RemoteException;
/**
 Created by kurila on 28.04.14.
 A builder pattern implementation which should help to instantiate a configured load executor.
 */
public interface LoadBuilder<T extends Item, U extends LoadExecutor<T>>
extends Closeable, Cloneable {
	//
	String
		MSG_TMPL_NOT_SPECIFIED = "\"{}\" parameter is not specified nor in configuration files neither in command line",
		MSG_TMPL_INVALID_VALUE = "illegal value specified for \"{}\" parameter: {}";
	//
	LoadBuilder<T, U> setAppConfig(final AppConfig appConfig)
	throws IllegalStateException, RemoteException;
	//
	IOConfig<?, ?> getIOConfig()
	throws RemoteException;
	LoadBuilder<T, U> setIOConfig(final IOConfig<?, ?> reqConf)
	throws RemoteException;
	//
	LoadBuilder<T, U> setLoadType(final IOTask.Type loadType)
	throws IllegalStateException, RemoteException;
	//
	LoadBuilder<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setRateLimit(final float rateLimit)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setThreadCount(final int threadCount)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setNodeAddrs(final String[] nodeAddrs)
	throws IllegalArgumentException, RemoteException;
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
	void invokePreConditions()
	throws RemoteException, IllegalStateException;
	//
	U build()
	throws IOException;
	//
}
