package com.emc.mongoose;
//
import com.emc.mongoose.data.UniformData;
//
import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 09.05.14.
 */
public interface Consumer<T extends UniformData>
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
