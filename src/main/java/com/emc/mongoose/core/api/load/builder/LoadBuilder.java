package com.emc.mongoose.core.api.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.item.container.Container;
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
	IoConfig<? extends Item, ? extends Container<? extends Item>> getIoConfig()
	throws RemoteException;
	LoadBuilder<T, U> setIoConfig(
		final IoConfig<? extends Item, ? extends Container<? extends Item>> ioConfig
	) throws RemoteException;
	//
	LoadBuilder<T, U> setLoadType(final LoadType loadType)
	throws IllegalStateException, RemoteException;
	//
	LoadBuilder<T, U> setCountLimit(final long countLimit)
	throws IllegalArgumentException, RemoteException;
	//
	LoadBuilder<T, U> setSizeLimit(final long sizeLimit)
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
	LoadBuilder<T, U> setInput(final Input<T> itemInput)
	throws RemoteException;
	//
	LoadBuilder<T, U> setOutput(final Output<T> itemOutput)
	throws RemoteException;
	//
	void invokePreConditions()
	throws RemoteException, IllegalStateException;
	//
	U build()
	throws IOException;
	//
}
