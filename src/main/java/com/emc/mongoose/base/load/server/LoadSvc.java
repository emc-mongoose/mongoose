package com.emc.mongoose.base.load.server;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.remote.RecordFrameBuffer;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A remote/server-side load executor.
 */
public interface LoadSvc<T extends DataItem>
extends LoadExecutor<T>, ConsumerSvc<T>, ProducerSvc<T>, RecordFrameBuffer<T> {
	int getTotalConnCount()
	throws RemoteException;
}

