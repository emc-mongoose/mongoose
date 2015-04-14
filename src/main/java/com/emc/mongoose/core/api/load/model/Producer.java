package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A producer feeding the generated data items to its consumer.
 May be linked with particular consumer, started and interrupted.
 */
public interface Producer<T extends DataItem> {
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
	void join()
	throws RemoteException, InterruptedException;
	//
	void join(final long ms)
	throws RemoteException, InterruptedException;
	//
	void interrupt()
	throws RemoteException;
	//
}
