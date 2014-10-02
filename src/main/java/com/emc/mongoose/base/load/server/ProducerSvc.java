package com.emc.mongoose.base.load.server;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.util.remote.Service;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 30.05.14.
 A remote/server-size data items producer.
 */
public interface ProducerSvc<T extends DataItem>
extends Producer<T>, Service {
	//
	void setConsumer(final ConsumerSvc<T> consumer)
	throws RemoteException;
	//
}
