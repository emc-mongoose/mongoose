package com.emc.mongoose.core.api.load.model;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 03.06.15.
 */
public interface AsyncConsumer<T>
extends Consumer<T> {
	void start()
	throws RemoteException, IllegalThreadStateException;
}
