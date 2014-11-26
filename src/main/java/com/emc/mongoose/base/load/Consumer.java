package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
//
import java.io.Closeable;
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A data items consumer supporting the method to feed the data item to it.
 The count of the consumed data items may be limited externally.
 Also supports closing which may be necessary to perform a cleanup.
 */
public interface Consumer<T extends DataItem>
extends Closeable {
	//
	void submit(final T data)
	throws RemoteException, InterruptedException;
	//
	long getMaxCount()
	throws RemoteException;
	//
	void setMaxCount(final long maxCount)
	throws RemoteException;
}
