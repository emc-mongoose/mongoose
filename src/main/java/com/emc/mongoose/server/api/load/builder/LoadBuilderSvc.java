package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.common.net.Service;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A remote/server-side load builder.
 */
public interface LoadBuilderSvc<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilder<T, U>, Service {
	//
	String buildRemotely()
	throws RemoteException;
	//
	int getNextInstanceNum()
	throws RemoteException;
	//
	void setNextInstanceNum(final int instanceN)
	throws RemoteException;
	//
}
