package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.item.base.Item;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A producer feeding the generated items to its consumer.
 May be linked with particular consumer, started and interrupted.
 */
public interface ItemProducer<T extends Item> {
	//
	void setOutput(final Output<T> itemDst)
	throws RemoteException;
	//
	void setSkipCount(final long itemsCount)
	throws RemoteException;
	//
	void setLastItem(final T dataItem)
	throws RemoteException;
	//
	void reset()
	throws RemoteException;
}
