package com.emc.mongoose.remote;
//
import com.emc.mongoose.Producer;
import com.emc.mongoose.data.UniformData;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 30.05.14.
 */
public interface ProducerService<T extends UniformData>
extends Producer<T>, Service {
	//
	void setConsumer(final ConsumerService<T> consumer)
	throws RemoteException;
	//
}
