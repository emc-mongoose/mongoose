package com.emc.mongoose.core.api.load.model;
//
import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 09.05.14.
 A data items consumer supporting the method to feed the data item to it.
 The count of the consumed data items may be limited externally.
 Also supports closing which may be necessary to perform a cleanup.
 */
public interface Consumer<T>
extends Closeable {
	//
	void feed(final T item)
	throws RemoteException, InterruptedException, RejectedExecutionException;
	//
	void feedBatch(final List<T> items)
	throws RemoteException, InterruptedException, RejectedExecutionException;
	//
	void shutdown()
	throws RemoteException;
	//
	long getMaxCount()
	throws RemoteException;
}
