package com.emc.mongoose.server.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
//
import java.rmi.RemoteException;
import java.util.List;
/**
 Created by kurila on 25.06.14.
 */
public interface RemoteItemBuffDst<T extends DataItem>
extends DataItemDst<T> {
	//
	List<T> fetchItems()
	throws RemoteException, InterruptedException;
}
