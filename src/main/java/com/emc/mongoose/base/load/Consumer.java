package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
//
import java.io.Closeable;
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 */
public interface Consumer<T extends DataItem>
extends Closeable {
	//
	void submit(final T data)
	throws RemoteException;
	//
	long getMaxCount()
	throws RemoteException;
	//
	void setMaxCount(final long maxCount)
	throws RemoteException;
	//
}
