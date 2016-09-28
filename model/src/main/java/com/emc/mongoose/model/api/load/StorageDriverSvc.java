package com.emc.mongoose.model.api.load;

import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;

import java.rmi.RemoteException;

/**
 Created on 28.09.16.
 */
public interface StorageDriverSvc<I extends Item, O extends IoTask<I>>
extends StorageDriver<I, O>, Service {

	void registerRemotely(final String monitorSvcName)
	throws RemoteException;
	
}
