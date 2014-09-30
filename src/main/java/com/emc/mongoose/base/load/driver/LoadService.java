package com.emc.mongoose.base.load.driver;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.util.remote.RecordFrameBuffer;
//
import java.rmi.RemoteException;
//
/**
 Created by kurila on 09.05.14.
 */
public interface LoadService<T extends DataItem>
extends LoadExecutor<T>, ConsumerService<T>, ProducerService<T>, RecordFrameBuffer<T> {
	int getThreadCount()
	throws RemoteException;
}

