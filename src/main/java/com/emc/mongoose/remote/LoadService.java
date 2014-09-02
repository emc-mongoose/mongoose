package com.emc.mongoose.remote;
//
import com.emc.mongoose.LoadExecutor;
import com.emc.mongoose.data.UniformData;

import java.rmi.RemoteException;
//
/**
 Created by kurila on 09.05.14.
 */
public interface LoadService<T extends UniformData>
extends LoadExecutor<T>, ConsumerService<T>, ProducerService<T>, RecordFrameBuffer<T> {
	int getThreadCount()
	throws RemoteException;
}

