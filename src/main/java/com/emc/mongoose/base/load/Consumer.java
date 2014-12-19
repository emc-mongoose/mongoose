package com.emc.mongoose.base.load;
//
import com.emc.mongoose.base.data.DataItem;
//
import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
	void shutdown()
	throws RemoteException;
	//
	boolean awaitTermination(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException;
	//
	List<Runnable> shutdownNow();
	//
	long getMaxCount()
	throws RemoteException;
}
