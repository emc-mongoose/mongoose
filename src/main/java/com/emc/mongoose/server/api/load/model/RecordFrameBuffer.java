package com.emc.mongoose.server.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.rmi.RemoteException;
import java.util.List;
/**
 Created by kurila on 25.06.14.
 */
public interface RecordFrameBuffer<T extends DataItem> {
	//
	List<T> takeFrame()
	throws RemoteException;
	//
}
