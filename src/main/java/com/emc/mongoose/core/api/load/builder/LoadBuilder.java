package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.io.conf.IOConfig;
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
	IOConfig<?, ?> getIoConfig()
	throws RemoteException;
	LoadBuilder<T, U> setIoConfig(final IOConfig<?, ?> reqConf)
	throws RemoteException;
	//
	LoadBuilder<T, U> setLoadType(final LoadType loadType)
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
	LoadBuilder<T, U> setInput(final Input<T> itemSrc)
	throws RemoteException;
	//
	LoadBuilder<T, U> setOutput(final Output<T> itemDst)
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
