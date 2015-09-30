package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.common.net.Service;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import java.rmi.RemoteException;
import java.util.List;
/**
 Created by kurila on 09.05.14.
 A remote/server-side load executor.
 */
public interface LoadSvc<T extends DataItem>
extends LoadExecutor<T>, Service {
	//
	int getInstanceNum()
	throws RemoteException;
	//
	List<T> getProcessedItems()
	throws RemoteException;
}

