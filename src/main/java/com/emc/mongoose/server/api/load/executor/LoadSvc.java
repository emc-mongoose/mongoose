package com.emc.mongoose.server.api.load.executor;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.server.api.load.model.ConsumerSvc;
import com.emc.mongoose.server.api.load.model.ProducerSvc;
import com.emc.mongoose.server.api.load.model.RemoteItemBuffDst;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A remote/server-side load executor.
 */
public interface LoadSvc<T extends DataItem>
extends LoadExecutor<T>, ConsumerSvc<T>, ProducerSvc<T>, RemoteItemBuffDst<T> {
	int getInstanceNum()
	throws RemoteException;
}

