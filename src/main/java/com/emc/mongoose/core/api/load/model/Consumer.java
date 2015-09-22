package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 03.06.15.
 */
public interface Consumer<T extends DataItem>
extends DataItemDst<T> {
	//
	void start()
	throws RemoteException, IllegalThreadStateException;
	//
	void shutdown()
	throws RemoteException, IllegalStateException;
	//
	void interrupt()
	throws RemoteException;
}
