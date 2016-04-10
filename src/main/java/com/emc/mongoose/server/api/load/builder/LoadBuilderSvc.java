package com.emc.mongoose.server.api.load.builder;
//
import com.emc.mongoose.core.api.v1.item.base.Item;
import com.emc.mongoose.core.api.v1.load.builder.LoadBuilder;
//
import com.emc.mongoose.common.net.Service;
//
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A remote/server-side load builder.
 */
public interface LoadBuilderSvc<T extends Item, U extends LoadSvc<T>>
extends LoadBuilder<T, U>, Service {
	//
	int fork()
	throws RemoteException;
	//
	String buildRemotely()
	throws RemoteException;
	//
	int getNextInstanceNum(final String runId)
	throws RemoteException;
	//
	void setNextInstanceNum(final String runId, final int instanceN)
	throws RemoteException;
	//
}
