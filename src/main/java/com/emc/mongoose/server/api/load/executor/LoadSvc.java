package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.server.api.load.model.RemoteItemBuffDst;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A remote/server-side load executor.
 */
public interface LoadSvc<T extends DataItem>
extends LoadExecutor<T>, RemoteItemBuffDst<T>, Service {
	int getInstanceNum()
	throws RemoteException;
}

