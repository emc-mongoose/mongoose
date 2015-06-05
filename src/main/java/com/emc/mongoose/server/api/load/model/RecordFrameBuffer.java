package com.emc.mongoose.server.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
//
import java.rmi.RemoteException;
import java.util.Collection;
/**
 Created by kurila on 25.06.14.
 */
public interface RecordFrameBuffer<T extends DataItem>
extends AsyncConsumer<T> {
	//
	Collection<T> takeFrame()
	throws RemoteException, InterruptedException;
	//
}
