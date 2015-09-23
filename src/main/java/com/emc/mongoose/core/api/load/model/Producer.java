package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemDst;
import com.emc.mongoose.core.api.data.model.DataItemSrc;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 09.05.14.
 A producer feeding the generated items to its consumer.
 May be linked with particular consumer, started and interrupted.
 */
public interface Producer<T extends DataItem> {
	//
	void setDataItemDst(final DataItemDst<T> itemDst)
	throws RemoteException;
	//
	DataItemSrc<T> getDataItemSrc()
	throws RemoteException;
	//
	void setSkipCount(final long itemsCount)
	throws RemoteException;
	//
	void setLastDataItem(final T dataItem)
	throws RemoteException;
	//
	void reset()
	throws RemoteException;
}
