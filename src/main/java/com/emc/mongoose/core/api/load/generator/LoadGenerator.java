package com.emc.mongoose.core.api.load.generator;
//
import com.emc.mongoose.common.concurrent.LifeCycle;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.load.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;

import java.rmi.RemoteException;
/**
 Created by andrey on 08.04.16.
 */
public interface LoadGenerator<T extends Item, A extends IoTask<T>>
extends LifeCycle {
	//
	String getName()
	throws RemoteException;
	//
	void setName(final String name)
	throws RemoteException;
	//
	LoadState<T> getState()
	throws RemoteException;
	//
	void setState(final LoadState<T> loadState)
	throws RemoteException;
	//
	LoadType getLoadType()
	throws RemoteException;
	//
	void setLoadType(final LoadType loadType)
	throws RemoteException;
	//
	long getCountLimit()
	throws RemoteException;
	//
	void setCountLimit(final long count)
	throws RemoteException;
	//
	Output<T> getOutput()
	throws RemoteException;
	//
	void setOutput(final Output<T> output)
	throws RemoteException;
	//
	int getWeight()
	throws RemoteException;
	//
	void setWeight(final int relativeWeight)
	throws RemoteException;
	//
	boolean isCircular()
	throws RemoteException;
	//
	void setCircular(final boolean circularityFlag)
	throws RemoteException;
	//
	LoadExecutor<T, A> getLoadExecutor()
	throws RemoteException;
}
