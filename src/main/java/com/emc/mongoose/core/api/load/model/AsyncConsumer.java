package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;

import java.rmi.RemoteException;
/**
 Created by kurila on 03.06.15.
 */
public interface AsyncConsumer<T extends DataItem>
extends Consumer<T> {
	void start()
	throws RemoteException, IllegalThreadStateException;
}
