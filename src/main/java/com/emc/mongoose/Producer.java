package com.emc.mongoose;
//
import com.emc.mongoose.data.UniformData;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 */
public interface Producer<T extends UniformData> {
	//
	void setConsumer(final Consumer<T> consumer)
	throws RemoteException;
	//
	Consumer<T> getConsumer()
	throws RemoteException;
	//
	void start()
	throws RemoteException;
	//
	void interrupt()
	throws RemoteException;
	//
}
