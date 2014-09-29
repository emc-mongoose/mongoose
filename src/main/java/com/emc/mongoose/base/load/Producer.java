package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
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
	void interrupt()
	throws RemoteException;
	//
}
