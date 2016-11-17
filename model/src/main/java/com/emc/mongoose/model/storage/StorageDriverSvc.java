package com.emc.mongoose.model.storage;

import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;

import java.rmi.RemoteException;

/**
 Created by andrey on 05.10.16.
 */
public interface StorageDriverSvc<I extends Item, O extends IoTask<I>, R extends IoResult>
extends StorageDriver<I, O, R>, Service {

	void setOutputSvc(final String addr, final String svcName)
	throws RemoteException;
}
