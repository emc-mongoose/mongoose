package com.emc.mongoose.base.load.driver;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.util.remote.Service;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 30.05.14.
 */
public interface ProducerService<T extends DataItem>
extends Producer<T>, Service {
	//
	void setConsumer(final ConsumerService<T> consumer)
	throws RemoteException;
	//
}
