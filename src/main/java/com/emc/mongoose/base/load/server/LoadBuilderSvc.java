package com.emc.mongoose.base.load.server;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.remote.Service;
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
	int getLastInstanceNum()
	throws RemoteException;
	//
	void setLastInstanceNum(final int instanceN)
	throws RemoteException;
}
