package com.emc.mongoose.core.api.load.model;
//
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 09.05.14.
 A producer feeding the generated items to its consumer.
 May be linked with particular consumer, started and interrupted.
 */
public interface Producer<T> {
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
	void await()
	throws RemoteException, InterruptedException;
	//
	void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException;
	//
	void interrupt()
	throws RemoteException;
}
