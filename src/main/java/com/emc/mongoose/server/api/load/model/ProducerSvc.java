package com.emc.mongoose.server.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Producer;
//
import com.emc.mongoose.common.net.Service;
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
