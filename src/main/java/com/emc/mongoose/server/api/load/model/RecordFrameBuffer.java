package com.emc.mongoose.server.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Consumer;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 25.06.14.
 */
public interface RecordFrameBuffer<T extends DataItem>
extends Consumer<T> {
	//
	T[] takeFrame()
	throws RemoteException, InterruptedException;
	//
}
